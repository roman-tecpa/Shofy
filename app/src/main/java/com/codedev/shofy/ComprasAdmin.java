package com.codedev.shofy;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.DB.DBVentas;
import com.codedev.shofy.adapters.ComprasAdapter;
import com.codedev.shofy.models.CompraDetalle;
import com.codedev.shofy.models.CompraResumen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ComprasAdmin extends Fragment {

    private RecyclerView recycler;
    private ComprasAdapter adapter;
    private EditText etDesde, etHasta;
    private TextView btnFiltrar;

    // Chips de periodo rápido
    private TextView chipHoy, chipSemana, chipMes;

    // Rango en ISO para SQLite
    private String desdeIso = "1970-01-01 00:00:00";
    private String hastaIso = "2100-12-31 23:59:59";

    public ComprasAdmin() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_compras_admin, container, false);

        recycler = v.findViewById(R.id.recyclerCompras);
        recycler.setLayoutManager(new LinearLayoutManager(getContext())); // 1 columna

        adapter = new ComprasAdapter(
                new ArrayList<>(),
                compra -> {
                    // Click en la tarjeta completa (si quieres abrir detalle)
                    Toast.makeText(getContext(), "Compra #" + compra.getIdVenta(), Toast.LENGTH_SHORT).show();
                },
                compra -> { // Click en el cuadrito verde "PDF"
                    DBVentas db = new DBVentas(getContext());
                    CompraDetalle detalle = db.obtenerDetalleVenta(compra.getIdVenta());
                    if (detalle == null) {
                        Toast.makeText(getContext(), "No se encontró el detalle.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File pdf = generarTiketPDF(detalle);
                    if (pdf != null) {
                        Uri uri = FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().getPackageName() + ".fileprovider",
                                pdf
                        );
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "application/pdf");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, "Abrir ticket"));
                    }
                }
        );
        recycler.setAdapter(adapter);

        etDesde = v.findViewById(R.id.etDesde);
        etHasta = v.findViewById(R.id.etHasta);
        btnFiltrar = v.findViewById(R.id.btnFiltrar);

        // Chips
        chipHoy = v.findViewById(R.id.chipHoy);
        chipSemana = v.findViewById(R.id.chipSemana);
        chipMes = v.findViewById(R.id.chipMes);

        // DatePickers
        etDesde.setOnClickListener(view -> abrirDatePicker(true));
        etHasta.setOnClickListener(view -> abrirDatePicker(false));
        btnFiltrar.setOnClickListener(view -> cargarCompras());

        // Listeners chips
        View[] chips = new View[]{chipHoy, chipSemana, chipMes};
        Runnable clearChips = () -> {
            if (chips[0] == null) return;
            for (View chip : chips) {
                if (chip != null) chip.setBackgroundResource(R.drawable.bg_category);
            }
        };

        if (chipHoy != null) {
            chipHoy.setOnClickListener(view -> {
                clearChips.run();
                view.setBackgroundResource(R.drawable.bg_category_selected);
                String[] rango = RangosFecha.hoy();
                desdeIso = rango[0];
                hastaIso = rango[1];
                etDesde.setText(RangosFecha.pretty(desdeIso));
                etHasta.setText(RangosFecha.pretty(hastaIso));
                cargarCompras();
            });
        }

        if (chipSemana != null) {
            chipSemana.setOnClickListener(view -> {
                clearChips.run();
                view.setBackgroundResource(R.drawable.bg_category_selected);
                String[] rango = RangosFecha.semanaActual();
                desdeIso = rango[0];
                hastaIso = rango[1];
                etDesde.setText(RangosFecha.pretty(desdeIso));
                etHasta.setText(RangosFecha.pretty(hastaIso));
                cargarCompras();
            });
        }

        if (chipMes != null) {
            chipMes.setOnClickListener(view -> {
                clearChips.run();
                view.setBackgroundResource(R.drawable.bg_category_selected);
                String[] rango = RangosFecha.mesActual();
                desdeIso = rango[0];
                hastaIso = rango[1];
                etDesde.setText(RangosFecha.pretty(desdeIso));
                etHasta.setText(RangosFecha.pretty(hastaIso));
                cargarCompras();
            });
        }

        // Carga inicial
        cargarCompras();
        return v;
    }

    private void abrirDatePicker(boolean esDesde) {
        final Calendar cal = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(
                getContext(),
                (picker, y, m, d) -> {
                    String ddmmyy = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
                    String isoIni = String.format(Locale.getDefault(), "%04d-%02d-%02d 00:00:00", y, m + 1, d);
                    String isoFin = String.format(Locale.getDefault(), "%04d-%02d-%02d 23:59:59", y, m + 1, d);
                    if (esDesde) {
                        etDesde.setText(ddmmyy);
                        desdeIso = isoIni;
                    } else {
                        etHasta.setText(ddmmyy);
                        hastaIso = isoFin;
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void cargarCompras() {
        DBVentas db = new DBVentas(getContext());
        ArrayList<CompraResumen> lista = db.listarComprasResumenRango(desdeIso, hastaIso);
        adapter.actualizar(lista);
    }

    /** Genera y guarda el PDF del ticket. Devuelve el File si fue exitoso. */
    private File generarTiketPDF(CompraDetalle detalle) {
        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // Tamaño tipo recibo
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setTextSize(16);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Ticket de Venta", 80, 24, titlePaint);

        paint.setTextSize(10);
        int y = 44;
        canvas.drawText("Venta #" + detalle.getIdVenta(), 10, y, paint); y += 16;
        canvas.drawText("Fecha: " + detalle.getFecha(), 10, y, paint); y += 16;
        if (detalle.getCliente() != null && !detalle.getCliente().isEmpty()) {
            canvas.drawText("Cliente: " + detalle.getCliente(), 10, y, paint); y += 16;
        } else { y += 4; }

        canvas.drawText("Producto", 10, y, paint);
        canvas.drawText("Cant x P.U.", 170, y, paint);
        y += 12; canvas.drawLine(10, y, 290, y, paint); y += 10;

        for (CompraDetalle.ProductoLinea p : detalle.getProductos()) {
            canvas.drawText(p.getNombre(), 10, y, paint);
            String cantPu = p.getCantidad() + " x $" + String.format(Locale.getDefault(), "%.2f", p.getPrecioUnitario());
            canvas.drawText(cantPu, 170, y, paint);
            y += 14;
        }

        y += 10;
        paint.setFakeBoldText(true);
        canvas.drawText("TOTAL: $" + String.format(Locale.getDefault(),"%.2f", detalle.getTotal()), 10, y, paint);

        pdf.finishPage(page);

        File out = new File(requireContext().getExternalFilesDir(null), "ticket_" + detalle.getIdVenta() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            pdf.writeTo(fos);
            Toast.makeText(getContext(), "Ticket guardado: " + out.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al generar PDF", Toast.LENGTH_SHORT).show();
            out = null;
        } finally {
            pdf.close();
        }
        return out;
    }

    // ===== Helpers de rangos (integrados) =====
    public static class RangosFecha {
        public static String[] hoy() {
            Calendar c = Calendar.getInstance();
            return diaCompleto(c);
        }
        public static String[] semanaActual() {
            Calendar c1 = Calendar.getInstance();
            c1.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            zero(c1);
            Calendar c2 = (Calendar) c1.clone();
            c2.add(Calendar.DAY_OF_YEAR, 6);
            endOfDay(c2);
            return new String[]{toIso(c1), toIso(c2)};
        }
        public static String[] mesActual() {
            Calendar c1 = Calendar.getInstance();
            c1.set(Calendar.DAY_OF_MONTH, 1);
            zero(c1);
            Calendar c2 = (Calendar) c1.clone();
            c2.set(Calendar.DAY_OF_MONTH, c2.getActualMaximum(Calendar.DAY_OF_MONTH));
            endOfDay(c2);
            return new String[]{toIso(c1), toIso(c2)};
        }
        public static String pretty(String iso) {
            try {
                java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return out.format(in.parse(iso));
            } catch (Exception e) { return iso; }
        }
        private static String[] diaCompleto(Calendar c) {
            Calendar from = (Calendar) c.clone(); zero(from);
            Calendar to = (Calendar) c.clone();   endOfDay(to);
            return new String[]{toIso(from), toIso(to)};
        }
        private static void zero(Calendar c) { c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); }
        private static void endOfDay(Calendar c) { c.set(Calendar.HOUR_OF_DAY,23); c.set(Calendar.MINUTE,59); c.set(Calendar.SECOND,59); c.set(Calendar.MILLISECOND,999); }
        private static String toIso(Calendar c) {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return f.format(c.getTime());
        }
    }
}
