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
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

public class DashboardKpiFragment extends Fragment {

    // ===== KPIs / Vistas =====
    private TextView tvMasVendido, tvMenosVendido, tvTotalIngresos, tvPromedioUnidad;
    private BarChart chartUnidades, chartIngresos;
    private PieChart chartIngresosPie;

    // AHORA: también son BarChart (verticales)
    private BarChart chartInvPorCategoria, chartConteoPorCategoria;

    private TextView tvInventarioPorCategoria, tvAgrupacionPorCategoria;

    // ===== Filtro de fechas =====
    private TextView tvResumenRango, tvDesde, tvHasta;
    private Button btnHoy, btnSemana, btnMes, btnTodo, btnDesde, btnHasta, btnAplicarRango;

    private DBHelper dbHelper;

    // Estado del rango
    private long desdeMs = 0L;
    private long hastaMs = 0L;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Detección de columna fecha en Ventas
    private boolean ventasTieneFecha = false;
    private static final String COL_VENTAS_FECHA = "fecha_venta_millis";

    // ===== Formato moneda =====
    private final NumberFormat currencyMx = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    // ===== Paletas de color =====
    private final int fondoOscuro = Color.parseColor("#121212");
    private final int grisMedio = Color.parseColor("#B3B3B3");
    private final int blanco = Color.WHITE;

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
        chartInvPorCategoria = v.findViewById(R.id.chartInvPorCategoria);         // ahora BarChart
        chartConteoPorCategoria = v.findViewById(R.id.chartConteoPorCategoria);   // ahora BarChart

        tvMasVendido = v.findViewById(R.id.tvMasVendido);
        tvMenosVendido = v.findViewById(R.id.tvMenosVendido);
        tvTotalIngresos = v.findViewById(R.id.tvTotalIngresos);
        tvPromedioUnidad = v.findViewById(R.id.tvPromedioUnidad);
        chartUnidades = v.findViewById(R.id.chartUnidades);
        chartIngresos = v.findViewById(R.id.chartIngresos);
        chartIngresosPie = v.findViewById(R.id.chartIngresosPie);

        // Filtro de fechas (IDs del layout con el bloque que ya te pasé)
        tvResumenRango = v.findViewById(R.id.tvResumenRango);
        tvDesde = v.findViewById(R.id.tvDesde);
        tvHasta = v.findViewById(R.id.tvHasta);
        btnHoy = v.findViewById(R.id.btnHoy);
        btnSemana = v.findViewById(R.id.btnSemana);
        btnMes = v.findViewById(R.id.btnMes);
        btnTodo = v.findViewById(R.id.btnTodo);
        btnDesde = v.findViewById(R.id.btnDesde);
        btnHasta = v.findViewById(R.id.btnHasta);
        btnAplicarRango = v.findViewById(R.id.btnAplicarRango);

        int colorTextoClaro = Color.BLACK;
        if (tvMasVendido != null) tvMasVendido.setTextColor(colorTextoClaro);
        if (tvMenosVendido != null) tvMenosVendido.setTextColor(colorTextoClaro);
        if (tvTotalIngresos != null) tvTotalIngresos.setTextColor(colorTextoClaro);
        if (tvPromedioUnidad != null) tvPromedioUnidad.setTextColor(colorTextoClaro);
        if (tvInventarioPorCategoria != null) tvInventarioPorCategoria.setTextColor(colorTextoClaro);
        if (tvAgrupacionPorCategoria != null) tvAgrupacionPorCategoria.setTextColor(colorTextoClaro);

        // Estilo dark para TODOS los BarChart (incluye los 2 nuevos verticales)
        estilizarChartBase(chartUnidades);
        estilizarChartBase(chartIngresos);
        estilizarChartBase(chartInvPorCategoria);
        estilizarChartBase(chartConteoPorCategoria);
        estilizarPieBase(chartIngresosPie);

        dbHelper = new DBHelper(requireContext());
        ventasTieneFecha = columnaExiste("Ventas", COL_VENTAS_FECHA);

        setupRangoFechas();

        // Cargas
        cargarDatosYActualizarUI(desdeMs, hastaMs);
        cargarInventarioPorCategoria();
        cargarAgrupacionPorCategoria();
        cargarGraficasPorCategoria();  // ahora pinta en vertical
    }

    // ====== Filtro de fechas ======
    private void setupRangoFechas() {
        setHoy();

        btnHoy.setOnClickListener(v -> { setHoy(); toast("Rango: Hoy"); });
        btnSemana.setOnClickListener(v -> { setSemanaActual(); toast("Rango: Semana actual"); });
        btnMes.setOnClickListener(v -> { setMesActual(); toast("Rango: Mes actual"); });
        btnTodo.setOnClickListener(v -> { setTodo(); toast("Rango: Todo"); });

        btnDesde.setOnClickListener(v -> showDatePicker(true));
        btnHasta.setOnClickListener(v -> showDatePicker(false));

        btnAplicarRango.setOnClickListener(v -> {
            if (desdeMs <= 0 || hastaMs <= 0 || desdeMs > hastaMs) {
                toast("Selecciona un rango válido.");
                return;
            }
            cargarDatosYActualizarUI(desdeMs, hastaMs);
        });
    }

    private void setHoy() {
        long now = System.currentTimeMillis();
        desdeMs = startOfDay(now);
        hastaMs = endOfDay(now);
        actualizarLabels("Hoy");
    }

    private void setSemanaActual() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        desdeMs = cal.getTimeInMillis();

        Calendar cal2 = Calendar.getInstance();
        hastaMs = endOfDay(cal2.getTimeInMillis());

        actualizarLabels("Semana actual");
    }

    private void setMesActual() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        desdeMs = cal.getTimeInMillis();

        Calendar cal2 = Calendar.getInstance();
        hastaMs = endOfDay(cal2.getTimeInMillis());

        actualizarLabels("Mes actual");
    }

    private void setTodo() {
        desdeMs = 0L;
        hastaMs = System.currentTimeMillis();
        actualizarLabels("Todo");
    }

    private void actualizarLabels(String resumen) {
        if (tvResumenRango != null) tvResumenRango.setText(resumen);
        if (tvDesde != null) tvDesde.setText(desdeMs == 0 ? "--/--/----" : sdf.format(new Date(desdeMs)));
        if (tvHasta != null) tvHasta.setText(hastaMs == 0 ? "--/--/----" : sdf.format(new Date(hastaMs)));
    }

    private void showDatePicker(boolean esDesde) {
        final Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.YEAR, year);
                    c.set(Calendar.MONTH, month);
                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    long ms = esDesde ? startOfDay(c.getTimeInMillis()) : endOfDay(c.getTimeInMillis());
                    if (esDesde) { desdeMs = ms; } else { hastaMs = ms; }
                    actualizarLabels("Rango manual");
                },
                y, m, d
        );
        dlg.show();
    }

    private long startOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long endOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private boolean columnaExiste(String tabla, String columna) {
        boolean existe = false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + tabla + ")", null);
            while (c.moveToNext()) {
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                if (columna.equalsIgnoreCase(name)) { existe = true; break; }
            }
        } catch (Exception ignored) { }
        finally { if (c != null) c.close(); db.close(); }
        return existe;
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(getContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    // ===== Estilos base para BarChart =====
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

    // ===== Barras verticales por categoría =====
    private void pintarBarChartCategorias(BarChart chart,
                                          List<String> labels,
                                          List<Float> values,
                                          String titulo,
                                          boolean mostrarEnteros) {
        int limit = Math.min(labels.size(), 10);
        List<BarEntry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            entries.add(new BarEntry(i, values.get(i)));
            xLabels.add(labels.get(i));
        }

        BarDataSet dataSet = new BarDataSet(entries, titulo);
        dataSet.setDrawValues(true);
        dataSet.setColors(PALETA_BARRAS);
        dataSet.setValueTextColor(blanco);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        if (mostrarEnteros) {
            data.setValueFormatter(new ValueFormatter() {
                @Override public String getFormattedValue(float value) {
                    return String.valueOf(Math.round(value));
                }
            });
        }

        chart.setData(data);
        chart.setFitBars(true);
        chart.getAxisRight().setEnabled(false);

        // Eje Y (valores)
        chart.getAxisLeft().setGranularity(1f);
        chart.getAxisLeft().setTextColor(blanco);
        chart.getAxisLeft().setGridColor(Color.parseColor("#2A2A2A"));
        chart.getAxisLeft().setAxisLineColor(grisMedio);

        // Eje X (categorías)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelRotationAngle(315f); // rotación para etiquetas largas
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(blanco);
        xAxis.setAxisLineColor(grisMedio);

        chart.setBackgroundColor(fondoOscuro);
        chart.getLegend().setTextColor(blanco);
        chart.getDescription().setEnabled(false);

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

        pintarBarChartCategorias(
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

        pintarBarChartCategorias(
                chartConteoPorCategoria,
                labelsCount,
                valoresCount,
                "Productos por categoría (Top 10)",
                true
        );
    }

    // ==== Carga principal con (opcional) filtro de fechas ====
    private void cargarDatosYActualizarUI(long desde, long hasta) {
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

        String sql;
        String[] args;

        if (ventasTieneFecha && desde > 0 && hasta > 0) {
            sql = "SELECT p.nombre, " +
                    "IFNULL(SUM(dv.cantidad_vendida), 0) AS unidades, " +
                    "IFNULL(SUM(dv.cantidad_vendida * dv.precio_venta), 0.0) AS ingresos " +
                    "FROM Productos p " +
                    "LEFT JOIN DetalleVentas dv ON dv.id_producto = p.id " +
                    "LEFT JOIN Ventas v ON v.id = dv.id_venta " +
                    "WHERE v." + COL_VENTAS_FECHA + " BETWEEN ? AND ? " +
                    "GROUP BY p.id, p.nombre " +
                    "ORDER BY unidades DESC, p.nombre ASC";
            args = new String[]{ String.valueOf(desde), String.valueOf(hasta) };
        } else {
            sql = "SELECT p.nombre, " +
                    "IFNULL(SUM(dv.cantidad_vendida), 0) AS unidades, " +
                    "IFNULL(SUM(dv.cantidad_vendida * dv.precio_venta), 0.0) AS ingresos " +
                    "FROM Productos p " +
                    "LEFT JOIN DetalleVentas dv ON dv.id_producto = p.id " +
                    "GROUP BY p.id, p.nombre " +
                    "ORDER BY unidades DESC, p.nombre ASC";
            args = null;
        }

        Cursor c = db.rawQuery(sql, args);
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

        tvMasVendido.setText("Producto más vendido: " + masVendidoNombre + " (" + (masVendidoUnidades == Integer.MIN_VALUE ? 0 : masVendidoUnidades) + ")");
        tvMenosVendido.setText("Producto menos vendido: " + menosVendidoNombre + " (" + (menosVendidoUnidades == Integer.MAX_VALUE ? 0 : menosVendidoUnidades) + ")");
        tvTotalIngresos.setText("Total ingresos: " + currencyMx.format(totalIngresos));
        double promedio = (totalUnidades == 0) ? 0.0 : (totalIngresos / totalUnidades);
        tvPromedioUnidad.setText("Promedio por unidad: " + currencyMx.format(promedio));

        pintarBarChart(chartUnidades, labels, unidadesList, "Unidades por producto (Top 10)", true);
        pintarBarChart(chartIngresos, labels, ingresosList, "Ingresos por producto (Top 10)", false);
        pintarPieChartTopIngresos(chartIngresosPie, labels, ingresosList, 5);
    }

    private void cargarDatosYActualizarUI() { cargarDatosYActualizarUI(desdeMs, hastaMs); }

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
                @Override public String getFormattedValue(float value) {
                    return String.valueOf(Math.round(value));
                }
            });
        } else {
            data.setValueFormatter(new ValueFormatter() {
                @Override public String getFormattedValue(float value) {
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
            @Override public String getFormattedValue(float value) {
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
