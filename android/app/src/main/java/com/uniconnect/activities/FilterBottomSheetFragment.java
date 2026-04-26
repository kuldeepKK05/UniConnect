package com.uniconnect.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.uniconnect.R;

public class FilterBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnFiltersApplied {
        void apply(String category, String mode, int radius, String city, String university);
    }

    private OnFiltersApplied callback;

    public static FilterBottomSheetFragment newInstance() {
        return new FilterBottomSheetFragment();
    }

    public void setOnFiltersApplied(OnFiltersApplied cb) { this.callback = cb; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup chipCategory = view.findViewById(R.id.chipGroupCategory);
        ChipGroup chipMode     = view.findViewById(R.id.chipGroupMode);
        Spinner   spinnerRadius = view.findViewById(R.id.spinnerRadius);
        AutoCompleteTextView etCity = view.findViewById(R.id.etFilterCity);
        AutoCompleteTextView etUni  = view.findViewById(R.id.etFilterUniversity);
        MaterialButton btnApply  = view.findViewById(R.id.btnApplyFilters);
        MaterialButton btnClear  = view.findViewById(R.id.btnClearFilters);

        // Radius spinner
        String[] radii = {"10 km", "50 km", "100 km", "150 km"};
        spinnerRadius.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, radii));
        spinnerRadius.setSelection(1); // default 50 km

        btnApply.setOnClickListener(v -> {
            String cat  = getSelectedChipTag(chipCategory);
            String mode = getSelectedChipTag(chipMode);
            int radius  = parseRadius(spinnerRadius.getSelectedItem().toString());
            String city = etCity.getText() != null ? etCity.getText().toString().trim() : null;
            String uni  = etUni.getText()  != null ? etUni.getText().toString().trim()  : null;
            if (callback != null) callback.apply(cat, mode, radius,
                    city.isEmpty() ? null : city,
                    uni.isEmpty()  ? null : uni);
            dismiss();
        });

        btnClear.setOnClickListener(v -> {
            chipCategory.clearCheck();
            chipMode.clearCheck();
            spinnerRadius.setSelection(1);
            etCity.setText("");
            etUni.setText("");
            if (callback != null) callback.apply(null, null, 50, null, null);
            dismiss();
        });
    }

    private String getSelectedChipTag(ChipGroup group) {
        if (group.getCheckedChipId() == View.NO_ID) return null;
        Chip chip = group.findViewById(group.getCheckedChipId());
        return chip != null && chip.getTag() != null ? chip.getTag().toString() : null;
    }

    private int parseRadius(String label) {
        try { return Integer.parseInt(label.replace(" km", "").trim()); }
        catch (NumberFormatException e) { return 50; }
    }
}
