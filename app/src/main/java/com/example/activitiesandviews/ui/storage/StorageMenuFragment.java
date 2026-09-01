package com.example.activitiesandviews.ui.storage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.activitiesandviews.R;

public class StorageMenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_storage_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnRoom = view.findViewById(R.id.btnRoom);
        Button btnFiles = view.findViewById(R.id.btnFiles);
        Button btnEncryptedPrefs = view.findViewById(R.id.btnEncryptedPrefs);

        btnRoom.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_storageMenu_to_room));

        btnFiles.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_storageMenu_to_files));

        btnEncryptedPrefs.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_storageMenu_to_encryptedPrefs));
    }
}
