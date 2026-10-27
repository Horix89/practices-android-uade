package com.example.activitiesandviews.ui.qr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.activitiesandviews.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment que implementa la lectura de códigos QR usando CameraX y ML Kit.
 *
 * Flujo de responsabilidades:
 *   onCreate()  →  registra el launcher de permisos (debe hacerse antes de onStart)
 *   onViewCreated()  →  llama a pedirPermisoCamara()
 *   pedirPermisoCamara()  →  verifica/solicita permiso CAMERA
 *   iniciarCamara()  →  configura CameraX y el analizador de QR
 */
public class QrScanFragment extends Fragment {

    private static final String TAG = "QrScanFragment";

    // Vista donde CameraX renderiza el stream en tiempo real
    private PreviewView previewView;
    private TextView tvResultado;

    // Hilo dedicado para el análisis de imágenes; CameraX lo requiere separado del hilo principal
    private ExecutorService cameraExecutor;

    // BarcodeScanner se reutiliza entre frames para no recrearlo en cada análisis
    private BarcodeScanner barcodeScanner;

    /*
     * El launcher de permisos debe registrarse en onCreate() (o como campo),
     * ANTES de onStart(). Si se registra más tarde, Android lanza una excepción.
     */
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // ──────────────────────────────────────────────────────────────────────────
    // 1. onCreate: registrar el launcher de permisos
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * registerForActivityResult debe llamarse aquí (o como inicializador de campo),
         * nunca dentro de onViewCreated/onClick, porque el sistema necesita registrarlo
         * antes de que el Fragment sea recreado por rotación u otro cambio de configuración.
         */
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    // El usuario aceptó: ya podemos iniciar la cámara
                    iniciarCamara();
                } else {
                    Toast.makeText(requireContext(),
                        "Se necesita el permiso de cámara para escanear QR",
                        Toast.LENGTH_LONG).show();
                }
            }
        );

        // ML Kit: configurado solo para QR, descarta otros formatos y es más rápido
        BarcodeScannerOptions opciones = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build();
        barcodeScanner = BarcodeScanning.getClient(opciones);

        // Un solo hilo de análisis es suficiente; más hilos no aumentan la velocidad de lectura
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. onViewCreated: punto de entrada visual → llama a pedirPermisoCamara()
    // ──────────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.previewView);
        tvResultado = view.findViewById(R.id.tvResultado);

        // El flujo siempre arranca por pedirPermisoCamara(), que decide si ir directo
        // a iniciarCamara() o pasar por el diálogo de permiso primero
        pedirPermisoCamara();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. pedirPermisoCamara: verifica el permiso y decide el camino
    // ──────────────────────────────────────────────────────────────────────────

    private void pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // El permiso ya fue otorgado en una sesión anterior: ir directo
            iniciarCamara();
        } else {
            // Solicitar el permiso; el resultado llega al callback de requestPermissionLauncher
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. iniciarCamara: configura CameraX y enlaza el analizador de QR
    // ──────────────────────────────────────────────────────────────────────────

    private void iniciarCamara() {
        /*
         * ProcessCameraProvider es el punto de entrada de CameraX.
         * Se obtiene de forma asíncrona; el callback se ejecuta en el hilo principal
         * una vez que el proveedor está listo.
         */
        ListenableFuture<ProcessCameraProvider> futuro =
            ProcessCameraProvider.getInstance(requireContext());

        futuro.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = futuro.get();

                // Preview: conecta el stream de la cámara al PreviewView del layout
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageAnalysis: entrega cada frame al analyzer en cameraExecutor
                // STRATEGY_KEEP_ONLY_LATEST descarta frames si el analyzer no terminó el anterior,
                // evitando que se acumule una cola de imágenes pendientes
                ImageAnalysis analisis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

                analisis.setAnalyzer(cameraExecutor, this::analizarFrame);

                // Cámara trasera: la más adecuada para escanear QR físicos
                CameraSelector camara = CameraSelector.DEFAULT_BACK_CAMERA;

                // Desenlazar usos anteriores antes de volver a enlazar
                // (previene IllegalStateException si el fragment se recrea)
                cameraProvider.unbindAll();

                // bindToLifecycle: CameraX libera la cámara automáticamente cuando
                // el Fragment va a onPause/onDestroy; no hay que liberarla manualmente
                cameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(), camara, preview, analisis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error al obtener ProcessCameraProvider", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. analizarFrame: procesa cada frame con ML Kit
    // ──────────────────────────────────────────────────────────────────────────

    private void analizarFrame(@NonNull ImageProxy imageProxy) {
        /*
         * InputImage.fromMediaImage convierte el frame de CameraX en el formato
         * que ML Kit espera. El rotation degree alinea la imagen con la orientación
         * real del dispositivo para que el reconocimiento funcione en todas las rotaciones.
         */
        InputImage imagen = InputImage.fromMediaImage(
            imageProxy.getImage(),
            imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(imagen)
            .addOnSuccessListener(codigos -> {
                for (Barcode codigo : codigos) {
                    String valor = codigo.getRawValue();
                    if (valor != null) {
                        procesarResultado(valor);
                    }
                }
            })
            .addOnFailureListener(e -> Log.w(TAG, "Error en ML Kit barcode scan", e))
            .addOnCompleteListener(tarea -> {
                /*
                 * imageProxy.close() SIEMPRE debe llamarse al terminar el análisis,
                 * sin importar si hubo éxito o error.
                 * Si no se cierra, CameraX no puede entregar el siguiente frame
                 * y el pipeline se traba indefinidamente.
                 */
                imageProxy.close();
            });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. procesarResultado: intenta parsear como JSON y muestra el resultado
    // ──────────────────────────────────────────────────────────────────────────

    private void procesarResultado(String rawValue) {
        String mensaje;

        try {
            // Si el QR contiene JSON válido, lo mostramos formateado con indentación
            JSONObject json = new JSONObject(rawValue);
            mensaje = "JSON detectado:\n" + json.toString(2);
        } catch (JSONException e) {
            // No es JSON: mostrar el texto plano tal cual
            mensaje = "Texto: " + rawValue;
        }

        // Actualizar la UI desde el hilo principal (el análisis corre en cameraExecutor)
        String mensajeFinal = mensaje;
        requireActivity().runOnUiThread(() -> {
            tvResultado.setText(mensajeFinal);
            Toast.makeText(requireContext(), mensajeFinal, Toast.LENGTH_SHORT).show();
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ciclo de vida: liberar recursos al destruir el Fragment
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Apagar el executor para liberar el hilo de análisis
        cameraExecutor.shutdown();
        // Cerrar el cliente de ML Kit y liberar sus recursos nativos
        barcodeScanner.close();
    }
}
