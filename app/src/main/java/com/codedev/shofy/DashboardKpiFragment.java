package com.codedev.shofy;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.codedev.shofy.DB.DBHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.formatter.ValueFormatter;


import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardKpiFragment extends Fragment {

    private TextView tvMasVendido, tvMenosVendido, tvTotalIngresos, tvPromedioUnidad;
    private BarChart chartUnidades, chartIngresos;
    private DBHelper dbHelper;
    private PieChart chartIngresosPie;

    private final NumberFormat currencyMx = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard_kpi, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        tvMasVendido = v.findViewById(R.id.tvMasVendido);
        tvMenosVendido = v.findViewById(R.id.tvMenosVendido);
        tvTotalIngresos = v.findViewById(R.id.tvTotalIngresos);
        tvPromedioUnidad = v.findViewById(R.id.tvPromedioUnidad);
        chartUnidades = v.findViewById(R.id.chartUnidades);
        chartIngresos = v.findViewById(R.id.chartIngresos);
        chartIngresosPie = v.findViewById(R.id.chartIngresosPie); // ← AQUÍ

        dbHelper = new DBHelper(requireContext());
        cargarDatosYActualizarUI();
    }


    private void cargarDatosYActualizarUI() {
        // Agregados por producto
        List<String> labels = new ArrayList<>();
        List<Float> unidadesList = new ArrayList<>();
        List<Float> ingresosList = new ArrayList<>();

        double totalIngresos = 0.0;
        int totalUnidades = 0;

        String masVendidoNombre = "-";
        int masVendidoUnidades = Integer.MIN_VALUE;

        String menosVendidoNombre = "-";
        int menosVendidoUnidades = Integer.MAX_VALUE;

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT p.nombre, " +
                        "IFNULL(SUM(dv.cantidad_vendida), 0) AS unidades, " +
                        "IFNULL(SUM(dv.cantidad_vendida * dv.precio_venta), 0.0) AS ingresos " +
                        "FROM Productos p " +
                        "LEFT JOIN DetalleVentas dv ON dv.id_producto = p.id " +
                        "GROUP BY p.id, p.nombre " +
                        "ORDER BY unidades DESC, p.nombre ASC", null);

        while (c.moveToNext()) {
            String nombre = c.getString(0);
            int unidades = c.getInt(1);
            double ingresos = c.getDouble(2);

            labels.add(nombre);
            unidadesList.add((float) unidades);
            ingresosList.add((float) ingresos);

            totalIngresos += ingresos;
            totalUnidades += unidades;

            if (unidades > masVendidoUnidades) {
                masVendidoUnidades = unidades;
                masVendidoNombre = nombre;
            }
            if (unidades < menosVendidoUnidades) {
                menosVendidoUnidades = unidades;
                menosVendidoNombre = nombre;
            }
        }
        c.close();
        db.close();

        // KPIs
        tvMasVendido.setText("Producto más vendido: " + masVendidoNombre + " (" + (masVendidoUnidades == Integer.MIN_VALUE ? 0 : masVendidoUnidades) + ")");
        tvMenosVendido.setText("Producto menos vendido: " + menosVendidoNombre + " (" + (menosVendidoUnidades == Integer.MAX_VALUE ? 0 : menosVendidoUnidades) + ")");
        tvTotalIngresos.setText("Total ingresos: " + currencyMx.format(totalIngresos));
        double promedio = (totalUnidades == 0) ? 0.0 : (totalIngresos / totalUnidades);
        tvPromedioUnidad.setText("Promedio por unidad: " + currencyMx.format(promedio));

        // Gráfica: Unidades por producto (Top 10)
        pintarBarChart(chartUnidades, labels, unidadesList, "Unidades por producto (Top 10)", true);

        // Gráfica: Ingresos por producto (Top 10)
        pintarBarChart(chartIngresos, labels, ingresosList, "Ingresos por producto (Top 10)", false);
        pintarPieChartTopIngresos(chartIngresosPie, labels, ingresosList, 5);

    }

    private void pintarBarChart(BarChart chart, List<String> labels, List<Float> values, String titulo, boolean enteros) {
        int limit = Math.min(labels.size(), 10);
        List<BarEntry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            entries.add(new BarEntry(i, values.get(i)));
            xLabels.add(labels.get(i));
        }

        BarDataSet dataSet = new BarDataSet(entries, titulo);
        dataSet.setDrawValues(true);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        if (enteros) {
            data.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf(Math.round(value));
                }
            });
        }

        chart.setData(data);
        chart.setFitBars(true);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setGranularity(enteros ? 1f : 0.1f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelRotationAngle(315f);
        xAxis.setDrawGridLines(false);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        chart.getLegend().setEnabled(true);
        chart.animateY(800);
        chart.invalidate();
    }

    private void pintarPieChartTopIngresos(com.github.mikephil.charting.charts.PieChart pieChart,
                                           List<String> labels,
                                           List<Float> ingresosList,
                                           int topN) {
        // Construir pares (nombre, ingreso)
        List<int[]> idxVal = new ArrayList<>(); // [idx, valor*100 para ordenar entero]
        for (int i = 0; i < labels.size(); i++) {
            float val = ingresosList.get(i) == null ? 0f : ingresosList.get(i);
            // evitamos NaN
            if (Float.isNaN(val)) val = 0f;
            // guardamos índice y valor escalado para ordenar de forma estable
            idxVal.add(new int[]{ i, (int) Math.round(val * 100) });
        }
        // ordenar desc por ingreso
        idxVal.sort((a, b) -> Integer.compare(b[1], a[1]));

        // tomar top N
        int limit = Math.min(topN, idxVal.size());
        List<com.github.mikephil.charting.data.PieEntry> entries = new ArrayList<>();
        float totalTop = 0f;
        for (int k = 0; k < limit; k++) {
            int idx = idxVal.get(k)[0];
            float val = ingresosList.get(idx);
            if (val < 0f) val = 0f;
            totalTop += val;
            entries.add(new com.github.mikephil.charting.data.PieEntry(val, labels.get(idx)));
        }

        com.github.mikephil.charting.data.PieDataSet dataSet =
                new com.github.mikephil.charting.data.PieDataSet(entries, "Top " + limit + " por ingresos");
        dataSet.setSliceSpace(2f);
        dataSet.setDrawValues(true);

        com.github.mikephil.charting.data.PieData data =
                new com.github.mikephil.charting.data.PieData(dataSet);

        // Formato de valores: porcentaje dentro del top (no del total general)
        final float totalTopFinal = totalTop;
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (totalTopFinal <= 0.0001f) return "0%";
                float pct = (value / totalTopFinal) * 100f;
                return String.format(Locale.US, "%.0f%%", pct);
            }
        });

        data.setValueTextSize(12f);

        pieChart.setUsePercentValues(false); // ya formateamos nosotros
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.setCenterText("Ingresos Top " + limit);
        pieChart.setCenterTextSize(14f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(true);

        pieChart.setData(data);
        pieChart.animateY(900, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
        pieChart.invalidate();
    }

}
