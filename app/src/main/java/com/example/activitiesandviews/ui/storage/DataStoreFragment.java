package com.example.activitiesandviews.ui.storage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.fragment.app.Fragment;

import com.example.activitiesandviews.R;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class DataStoreFragment extends Fragment {

    private static final Preferences.Key<String> KEY_VALOR = PreferencesKeys.stringKey("valor_demo");

    @Inject
    RxDataStore<Preferences> dataStore;

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_store, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etValor = view.findViewById(R.id.etValor);
        Button btnGuardar = view.findViewById(R.id.btnGuardar);
        Button btnRecuperar = view.findViewById(R.id.btnRecuperar);
        Button btnLimpiar = view.findViewById(R.id.btnLimpiar);
        TextView tvResultado = view.findViewById(R.id.tvResultado);

        btnGuardar.setOnClickListener(v -> {
            String value = etValor.getText().toString().trim();
            if (value.isEmpty()) {
                tvResultado.setText("Ingresa un valor primero");
                return;
            }
            disposables.add(dataStore.updateDataAsync(prefsIn -> {
                        MutablePreferences mutable = prefsIn.toMutablePreferences();
                        mutable.set(KEY_VALOR, value);
                        return Single.just(mutable);
                    })
                    .subscribe(
                            prefs -> requireActivity().runOnUiThread(() ->
                                    tvResultado.setText("Guardado:\n\"" + value + "\"")),
                            throwable -> requireActivity().runOnUiThread(() ->
                                    tvResultado.setText("Error al guardar: " + throwable.getMessage()))
                    ));
        });

        btnRecuperar.setOnClickListener(v ->
                disposables.add(dataStore.data().firstOrError()
                        .subscribe(
                                prefs -> requireActivity().runOnUiThread(() -> {
                                    String stored = prefs.get(KEY_VALOR);
                                    if (stored != null) {
                                        tvResultado.setText("Recuperado:\n\"" + stored + "\"");
                                    } else {
                                        tvResultado.setText("No hay datos guardados");
                                    }
                                }),
                                throwable -> requireActivity().runOnUiThread(() ->
                                        tvResultado.setText("Error al leer: " + throwable.getMessage()))
                        )));

        btnLimpiar.setOnClickListener(v ->
                disposables.add(dataStore.updateDataAsync(prefsIn -> {
                            MutablePreferences mutable = prefsIn.toMutablePreferences();
                            mutable.remove(KEY_VALOR);
                            return Single.just(mutable);
                        })
                        .subscribe(
                                prefs -> requireActivity().runOnUiThread(() ->
                                        tvResultado.setText("Dato eliminado")),
                                throwable -> requireActivity().runOnUiThread(() ->
                                        tvResultado.setText("Error al limpiar: " + throwable.getMessage()))
                        )));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}
