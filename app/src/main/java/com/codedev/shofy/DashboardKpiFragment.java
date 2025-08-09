package com.codedev.shofy;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.DatePickerDialog;
import android.widget.Button;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.codedev.shofy.DB.DBHelper;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

public class DashboardKpiFragment extends Fragment {

    private TextView tvMasVendido, tvMenosVendido, tvTotalIngresos, tvPromedioUnidad;
    private BarChart chartUnidades, chartIngresos;
    private DBHelper dbHelper;
    private PieChart chartIngresosPie;
    private HorizontalBarChart chartInvPorCategoria, chartConteoPorCategoria;
    private TextView tvFechaDesde, tvFechaHasta;
    private Button btnAplicarFiltro, btnLimpiarFiltro;

    private final NumberFormat currencyMx = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private TextView tvInventarioPorCategoria, tvAgrupacionPorCategoria;

    // ===== Paletas de color =====
    private final int fondoOscuro = Color.parseColor("#121212");
    private final int grisMedio = Color.parseColor("#B3B3B3");
    private final int blanco = Color.WHITE;

    // Barras
    private final int[] PALETA_BARRAS = new int[]{
            Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#F44336"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#8BC34A"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#3F51B5")
    };

    // Pie
    private final int[] PALETA_PIE = new int[]{
            Color.parseColor("#3F51B5"),
            Color.parseColor("#009688"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#607D8B"),
            Color.parseColor("#FFEB3B")
    };

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

        tvInventarioPorCategoria = v.findViewById(R.id.tvInventarioPorCategoria);
        tvAgrupacionPorCategoria = v.findViewById(R.id.tvAgrupacionPorCategoria);
        chartInvPorCategoria = v.findViewById(R.id.chartInvPorCategoria);
        chartConteoPorCategoria = v.findViewById(R.id.chartConteoPorCategoria);

        tvMasVendido = v.findViewById(R.id.tvMasVendido);
        tvMenosVendido = v.findViewById(R.id.tvMenosVendido);
        tvTotalIngresos = v.findViewById(R.id.tvTotalIngresos);
        tvPromedioUnidad = v.findViewById(R.id.tvPromedioUnidad);
        chartUnidades = v.findViewById(R.id.chartUnidades);
        chartIngresos = v.findViewById(R.id.chartIngresos);
        chartIngresosPie = v.findViewById(R.id.chartIngresosPie);

        // ===== Opción A: KPIs y textos en fondo claro =====
        int colorTextoClaro = Color.BLACK;
        if (tvMasVendido != null) tvMasVendido.setTextColor(colorTextoClaro);
        if (tvMenosVendido != null) tvMenosVendido.setTextColor(colorTextoClaro);
        if (tvTotalIngresos != null) tvTotalIngresos.setTextColor(colorTextoClaro);
        if (tvPromedioUnidad != null) tvPromedioUnidad.setTextColor(colorTextoClaro);
        if (tvInventarioPorCategoria != null) tvInventarioPorCategoria.setTextColor(colorTextoClaro);
        if (tvAgrupacionPorCategoria != null) tvAgrupacionPorCategoria.setTextColor(colorTextoClaro);

        // Charts con look dark
        estilizarChartBase(chartUnidades);
        estilizarChartBase(chartIngresos);
        estilizarChartBase(chartInvPorCategoria);
        estilizarChartBase(chartConteoPorCategoria);
        estilizarPieBase(chartIngresosPie);

        dbHelper = new DBHelper(requireContext());
        cargarDatosYActualizarUI();
        cargarInventarioPorCategoria();
        cargarAgrupacionPorCategoria();
        cargarGraficasPorCategoria();
    }

    // ===== Estilos base =====
    private void estilizarChartBase(BarChart chart) {
        chart.setBackgroundColor(fondoOscuro);
        chart.setDrawGridBackground(false);
        chart.getLegend().setTextColor(blanco);
        chart.getDescription().setEnabled(false);

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setTextColor(blanco);
        chart.getAxisLeft().setGridColor(Color.parseColor("#2A2A2A"));
        chart.getAxisLeft().setAxisLineColor(grisMedio);

        XAxis x = chart.getXAxis();
        x.setTextColor(blanco);
        x.setGridColor(Color.parseColor("#2A2A2A"));
        x.setAxisLineColor(grisMedio);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
    }

    private void estilizarChartBase(HorizontalBarChart chart) {
        chart.setBackgroundColor(fondoOscuro);
        chart.setDrawGridBackground(false);
        chart.getLegend().setTextColor(blanco);
        chart.getDescription().setEnabled(false);

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setTextColor(blanco);
        chart.getAxisLeft().setGridColor(Color.parseColor("#2A2A2A"));
        chart.getAxisLeft().setAxisLineColor(grisMedio);

        XAxis x = chart.getXAxis();
        x.setTextColor(blanco);
        x.setGridColor(Color.parseColor("#2A2A2A"));
        x.setAxisLineColor(grisMedio);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        chart.setFitBars(true);
    }

    private void estilizarPieBase(PieChart pie) {
        pie.setUsePercentValues(false);
        pie.setDrawEntryLabels(true);
        pie.setEntryLabelColor(blanco);
        pie.setEntryLabelTextSize(11f);
        pie.setCenterTextColor(blanco);
        pie.setCenterTextSize(14f);
        pie.setDrawHoleEnabled(true);
        pie.setHoleColor(fondoOscuro);
        pie.setTransparentCircleColor(blanco);
        pie.setTransparentCircleAlpha(40);
        pie.getLegend().setTextColor(blanco);
        pie.getDescription().setEnabled(false);
        pie.setBackgroundColor(fondoOscuro);
        pie.setExtraOffsets(6f, 6f, 6f, 6f);
    }

    private void pintarHorizontalBarChart(HorizontalBarChart chart,
                                          List<String> labels,
                                          List<Float> values,
                                          String titulo,
                                          boolean mostrarEnteros) {
        int limit = Math.min(labels.size(), 10);
        List<com.github.mikephil.charting.data.BarEntry> entries = new ArrayList<>();
        List<String> yLabels = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            entries.add(new com.github.mikephil.charting.data.BarEntry(i, values.get(i)));
            yLabels.add(labels.get(i));
        }

        com.github.mikephil.charting.data.BarDataSet dataSet =
                new com.github.mikephil.charting.data.BarDataSet(entries, titulo);
        dataSet.setDrawValues(true);
        dataSet.setColors(PALETA_BARRAS);
        dataSet.setValueTextColor(blanco);
        dataSet.setValueTextSize(12f);

        com.github.mikephil.charting.data.BarData data =
                new com.github.mikephil.charting.data.BarData(dataSet);
        data.setBarWidth(0.6f);
        if (mostrarEnteros) {
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

        com.github.mikephil.charting.components.XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(blanco);
        xAxis.setAxisLineColor(grisMedio);

        com.github.mikephil.charting.components.YAxis yLeft = chart.getAxisLeft();
        yLeft.setGranularity(1f);
        yLeft.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(yLabels));
        yLeft.setDrawGridLines(false);
        yLeft.setTextColor(blanco);
        yLeft.setAxisLineColor(grisMedio);

        chart.getAxisRight().setEnabled(false);

        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextColor(blanco);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        chart.animateY(1000, Easing.EaseInOutQuad);
        chart.invalidate();
    }

    private void cargarGraficasPorCategoria() {
        // 1) Inventario por categoría
        List<String> labelsInv = new ArrayList<>();
        List<Float> valoresInv = new ArrayList<>();

        SQLiteDatabase db1 = dbHelper.getReadableDatabase();
        Cursor c1 = db1.rawQuery(
                "SELECT tipo, IFNULL(SUM(cantidad_actual), 0) AS unidades_stock " +
                        "FROM Productos " +
                        "GROUP BY tipo " +
                        "ORDER BY unidades_stock DESC, tipo ASC", null
        );
        while (c1.moveToNext()) {
            String tipo = c1.getString(0);
            int stock = c1.getInt(1);
            if (tipo == null || tipo.trim().isEmpty()) tipo = "Sin categoría";
            labelsInv.add(tipo);
            valoresInv.add((float) stock);
        }
        c1.close();
        db1.close();

        pintarHorizontalBarChart(
                chartInvPorCategoria,
                labelsInv,
                valoresInv,
                "Inventario por categoría (unidades en stock, Top 10)",
                true
        );

        // 2) Conteo de productos por categoría
        List<String> labelsCount = new ArrayList<>();
        List<Float> valoresCount = new ArrayList<>();

        SQLiteDatabase db2 = dbHelper.getReadableDatabase();
        Cursor c2 = db2.rawQuery(
                "SELECT tipo, COUNT(*) AS total_productos " +
                        "FROM Productos " +
                        "GROUP BY tipo " +
                        "ORDER BY total_productos DESC, tipo ASC", null
        );
        while (c2.moveToNext()) {
            String tipo = c2.getString(0);
            int total = c2.getInt(1);
            if (tipo == null || tipo.trim().isEmpty()) tipo = "Sin categoría";
            labelsCount.add(tipo);
            valoresCount.add((float) total);
        }
        c2.close();
        db2.close();

        pintarHorizontalBarChart(
                chartConteoPorCategoria,
                labelsCount,
                valoresCount,
                "Productos por categoría (Top 10)",
                true
        );
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

        // Pie: Top N ingresos
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
        dataSet.setColors(PALETA_BARRAS);
        dataSet.setValueTextColor(blanco);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        if (enteros) {
            data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf(Math.round(value));
                }
            });
        } else {
            data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (value >= 1000f) return String.valueOf(Math.round(value));
                    return String.format(Locale.US, "%.2f", value);
                }
            });
        }

        chart.setData(data);
        chart.setFitBars(true);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setGranularity(enteros ? 1f : 0.1f);
        chart.getAxisLeft().setTextColor(blanco);
        chart.getAxisLeft().setGridColor(Color.parseColor("#2A2A2A"));
        chart.getAxisLeft().setAxisLineColor(grisMedio);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelRotationAngle(315f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(blanco);
        xAxis.setAxisLineColor(grisMedio);

        chart.setBackgroundColor(fondoOscuro);
        chart.getLegend().setTextColor(blanco);
        chart.getDescription().setEnabled(false);

        chart.animateY(1100, Easing.EaseInOutQuad);
        chart.invalidate();
    }

    private void pintarPieChartTopIngresos(PieChart pieChart,
                                           List<String> labels,
                                           List<Float> ingresosList,
                                           int topN) {
        List<int[]> idxVal = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            float val = ingresosList.get(i) == null ? 0f : ingresosList.get(i);
            if (Float.isNaN(val)) val = 0f;
            idxVal.add(new int[]{ i, (int) Math.round(val * 100) });
        }
        idxVal.sort((a, b) -> Integer.compare(b[1], a[1]));

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
        dataSet.setColors(PALETA_PIE);
        dataSet.setValueTextColor(blanco);
        dataSet.setValueTextSize(12f);

        com.github.mikephil.charting.data.PieData data =
                new com.github.mikephil.charting.data.PieData(dataSet);

        final float totalTopFinal = totalTop;
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (totalTopFinal <= 0.0001f) return "0%";
                float pct = (value / totalTopFinal) * 100f;
                return String.format(Locale.US, "%.0f%%", pct);
            }
        });

        pieChart.setCenterText("Ingresos Top " + limit);
        pieChart.setData(data);
        pieChart.animateY(1000, Easing.EaseInOutBack);
        pieChart.invalidate();
    }

    private void cargarInventarioPorCategoria() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("Inventario por categoría (Stock):\n");

        Cursor c = db.rawQuery(
                "SELECT tipo, IFNULL(SUM(cantidad_actual), 0) AS unidades_stock " +
                        "FROM Productos " +
                        "GROUP BY tipo " +
                        "ORDER BY unidades_stock DESC, tipo ASC", null);

        while (c.moveToNext()) {
            String tipo = c.getString(0);
            int unidades = c.getInt(1);
            if (tipo == null || tipo.trim().isEmpty()) tipo = "Sin categoría";
            sb.append("• ").append(tipo).append(": ")
                    .append(unidades).append(" uds\n");
        }
        c.close();
        db.close();

        tvInventarioPorCategoria.setText(sb.toString());
    }

    private void cargarAgrupacionPorCategoria() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("Agrupación por categoría (número de productos):\n");

        Cursor c = db.rawQuery(
                "SELECT tipo, COUNT(*) AS total_productos " +
                        "FROM Productos " +
                        "GROUP BY tipo " +
                        "ORDER BY total_productos DESC, tipo ASC", null);

        while (c.moveToNext()) {
            String tipo = c.getString(0);
            int total = c.getInt(1);
            if (tipo == null || tipo.trim().isEmpty()) tipo = "Sin categoría";
            sb.append("• ").append(tipo).append(": ")
                    .append(total).append(" productos\n");
        }
        c.close();
        db.close();

        tvAgrupacionPorCategoria.setText(sb.toString());
    }
}
