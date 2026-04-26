package com.uniconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.uniconnect.R;
import com.uniconnect.models.ApiResponse;
import com.uniconnect.models.ApiResponses.City;
import com.uniconnect.network.ApiService;
import com.uniconnect.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationPickerActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvCities;
    private ProgressBar progressBar;
    private CityAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        etSearch = findViewById(R.id.etCitySearch);
        rvCities = findViewById(R.id.rvCities);
        progressBar = findViewById(R.id.layoutLoading);

        apiService = RetrofitClient.getInstance(this).getApiService();

        rvCities.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CityAdapter(new ArrayList<>(), city -> {
            Intent result = new Intent();
            result.putExtra("city", city.name);
            result.putExtra("lat", city.latitude);
            result.putExtra("lng", city.longitude);
            setResult(RESULT_OK, result);
            finish();
        });
        rvCities.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() >= 2) search(s.toString());
            }
        });
    }

    private void search(String query) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.searchCities(query).enqueue(new Callback<ApiResponse<List<City>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<City>>> call,
                                   Response<ApiResponse<List<City>>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setCities(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<City>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LocationPickerActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class CityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<City> cities;
        private OnCityClickListener listener;

        interface OnCityClickListener { void onCityClick(City city); }

        CityAdapter(List<City> cities, OnCityClickListener listener) {
            this.cities = cities;
            this.listener = listener;
        }

        void setCities(List<City> cities) {
            this.cities = cities != null ? cities : new ArrayList<>();
            notifyDataSetChanged();
        }

        @Override public int getItemCount() { return cities.size(); }
        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // Simplified
        }
        @Override public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            return null; // Simplified
        }
    }
}
