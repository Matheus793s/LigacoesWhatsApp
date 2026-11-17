package br.com.ligacoeswhatsapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * App local: o histórico nunca é enviado para servidores.
 * Quando o número não está cadastrado, o próprio WhatsApp exibe o aviso.
 */
public class MainActivity extends Activity {
    private static final int REQUEST_CALL_LOG = 18;
    private static final int MAX_CALLS = 1000;
    private static final int GREEN = Color.rgb(18, 140, 103);
    private static final int DARK = Color.rgb(29, 48, 43);
    private static final int MUTED = Color.rgb(103, 121, 114);
    private static final int LIGHT = Color.rgb(246, 248, 247);
    private static final int RED = Color.rgb(178, 53, 49);

    private final List<CallItem> allCalls = new ArrayList<>();
    private final List<CallItem> visibleCalls = new ArrayList<>();
    private HistoryAdapter adapter;
    private TextView statusView;
    private EditText searchInput;
    private boolean loadedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED) {
            loadCalls();
        } else {
            statusView.setText("Para mostrar suas ligações, autorize a leitura do histórico. Toque em Atualizar e aceite a permissão.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!loadedOnce && checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED && statusView != null) {
            loadCalls();
        }
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(12));
        root.setBackgroundColor(LIGHT);
        setContentView(root);

        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(headingRow, new LinearLayout.LayoutParams(-1, -2));

        TextView title = makeText("Ligações", 27, DARK, true);
        headingRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        Button refresh = smallButton("Atualizar", false);
        headingRow.addView(refresh);
        refresh.setOnClickListener(v -> requestAndLoad());

        TextView subtitle = makeText("Converse pelo WhatsApp sem salvar o número", 13, MUTED, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(5);
        subLp.bottomMargin = dp(15);
        root.addView(subtitle, subLp);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(15);
        searchInput.setHint("Buscar nome ou número");
        searchInput.setHintTextColor(MUTED);
        searchInput.setTextColor(DARK);
        searchInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        searchInput.setBackground(rounded(Color.WHITE, 14, Color.rgb(222, 234, 227)));
        root.addView(searchInput, new LinearLayout.LayoutParams(-1, dp(48)));
        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            public void afterTextChanged(Editable editable) { }
        });

        statusView = makeText("", 12, MUTED, false);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.topMargin = dp(12);
        statusLp.bottomMargin = dp(8);
        root.addView(statusView, statusLp);

        ListView listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(dp(8));
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setVerticalScrollBarEnabled(false);
        listView.setClipToPadding(false);
        adapter = new HistoryAdapter();
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button manual = smallButton("+ Conversar com outro número", true);
        LinearLayout.LayoutParams manualLp = new LinearLayout.LayoutParams(-1, dp(48));
        manualLp.topMargin = dp(9);
        root.addView(manual, manualLp);
        manual.setOnClickListener(v -> promptForNumber("") );

        TextView privacy = makeText("Privacidade: o histórico é lido somente neste aparelho.", 11, MUTED, false);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyLp = new LinearLayout.LayoutParams(-1, -2);
        privacyLp.topMargin = dp(8);
        root.addView(privacy, privacyLp);
    }

    private void requestAndLoad() {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED) {
            loadCalls();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_CALL_LOG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CALL_LOG) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadCalls();
        } else {
            statusView.setText("Acesso negado. Para ver as ligações, conceda a permissão de histórico de chamadas nas configurações do Android.");
            new AlertDialog.Builder(this)
                .setTitle("Permissão necessária")
                .setMessage("O Android precisa autorizar o acesso ao histórico. Se a opção estiver bloqueada, consulte as instruções do projeto. Você ainda pode digitar um número manualmente.")
                .setPositiveButton("Entendi", null)
                .show();
        }
    }

    private void loadCalls() {
        statusView.setText("Carregando histórico…");
        new Thread(() -> {
            List<CallItem> loaded = new ArrayList<>();
            String error = null;
            String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            };
            try (Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI, projection, null, null,
                    CallLog.Calls.DATE + " DESC")) {
                if (cursor != null) {
                    int numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER);
                    int nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME);
                    int typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE);
                    int dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE);
                    int durationIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION);
                    while (cursor.moveToNext() && loaded.size() < MAX_CALLS) {
                        String number = cursor.getString(numberIdx);
                        if (number == null || number.trim().isEmpty()) continue;
                        String name = cursor.getString(nameIdx);
                        int type = cursor.getInt(typeIdx);
                        long when = cursor.getLong(dateIdx);
                        long duration = Math.max(0, cursor.getLong(durationIdx));
                        loaded.add(new CallItem(name, number, type, when, duration));
                    }
                }
            } catch (SecurityException e) {
                error = "O Android bloqueou a leitura do histórico. Confira a permissão de acesso às chamadas nas configurações.";
            } catch (Exception e) {
                error = "Não foi possível ler o histórico neste aparelho.";
            }
            String finalError = error;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                loadedOnce = true;
                allCalls.clear();
                allCalls.addAll(loaded);
                applyFilter();
                if (finalError != null) statusView.setText(finalError);
            });
        }).start();
    }

    private void applyFilter() {
        if (adapter == null || statusView == null || searchInput == null) return;
        String search = searchInput.getText().toString().toLowerCase(Locale.ROOT).trim();
        String searchDigits = search.replaceAll("[^0-9]", "");
        visibleCalls.clear();
        for (CallItem item : allCalls) {
            String name = item.name == null ? "" : item.name.toLowerCase(Locale.ROOT);
            String number = item.number.toLowerCase(Locale.ROOT);
            if (search.isEmpty() || name.contains(search) || number.contains(search)
                    || (!searchDigits.isEmpty() && number.replaceAll("[^0-9]", "").contains(searchDigits))) {
                visibleCalls.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        if (allCalls.isEmpty()) {
            statusView.setText("Nenhuma ligação encontrada. Se o acesso estiver bloqueado, confira as permissões do Android.");
        } else {
            statusView.setText(visibleCalls.size() + " ligações exibidas • mais recentes primeiro");
        }
    }

    private void openWhatsApp(String rawNumber) {
        String digits = PhoneNumbers.whatsappDigits(rawNumber);
        if (digits == null) {
            promptForNumber(rawNumber);
            return;
        }
        String pkg = null;
        if (installed("com.whatsapp")) pkg = "com.whatsapp";
        else if (installed("com.whatsapp.w4b")) pkg = "com.whatsapp.w4b";

        if (pkg == null) {
            new AlertDialog.Builder(this)
                .setTitle("WhatsApp não instalado")
                .setMessage("Este celular não possui WhatsApp ou WhatsApp Business instalado. Instale o aplicativo para iniciar conversas.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=" + digits));
        intent.setPackage(pkg);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            try {
                Intent alternative = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + digits));
                alternative.setPackage(pkg);
                startActivity(alternative);
            } catch (ActivityNotFoundException | SecurityException ignored) {
                new AlertDialog.Builder(this)
                    .setTitle("Não foi possível abrir")
                    .setMessage("O WhatsApp não conseguiu abrir a conversa. Verifique se está atualizado e tente novamente.")
                    .setPositiveButton("OK", null)
                    .show();
            }
        }
        // A API pública não fornece resposta se o DESTINATÁRIO usa WhatsApp.
        // Essa validação e a mensagem correspondente cabem ao próprio WhatsApp.
    }

    private boolean installed(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void promptForNumber(String initial) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setSingleLine(true);
        input.setText(initial == null ? "" : initial);
        input.setHint("Ex.: 5585999999999");
        input.setSelectAllOnFocus(true);
        LinearLayout padded = new LinearLayout(this);
        padded.setPadding(dp(24), dp(5), dp(24), 0);
        padded.addView(input, new LinearLayout.LayoutParams(-1, -2));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Confira o número")
            .setMessage("Digite DDI + DDD + número, sem espaços. No Brasil: 55 + DDD + telefone.")
            .setView(padded)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Abrir WhatsApp", null)
            .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String raw = input.getText().toString();
            String normalized = PhoneNumbers.whatsappDigits(raw);
            if (normalized == null) {
                input.setError("Inclua o código de país e o DDD, por exemplo 5585999999999.");
            } else {
                dialog.dismiss();
                openWhatsApp("+" + normalized);
            }
        }));
        dialog.show();
    }

    private TextView makeText(String label, int sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(sp);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private Button smallButton(String text, boolean filled) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(13);
        btn.setAllCaps(false);
        btn.setTextColor(filled ? Color.WHITE : GREEN);
        btn.setBackground(rounded(filled ? GREEN : Color.WHITE, 14, Color.rgb(219, 231, 224)));
        btn.setPadding(dp(13), 0, dp(13), 0);
        btn.setMinHeight(dp(43));
        btn.setMinimumHeight(dp(43));
        return btn;
    }

    private GradientDrawable rounded(int color, int radiusDp, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radiusDp));
        shape.setStroke(dp(1), stroke);
        return shape;
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private static class CallItem {
        final String name;
        final String number;
        final int type;
        final long when;
        final long durationSeconds;
        CallItem(String name, String number, int type, long when, long durationSeconds) {
            this.name = name;
            this.number = number;
            this.type = type;
            this.when = when;
            this.durationSeconds = durationSeconds;
        }
    }

    private class HistoryAdapter extends BaseAdapter {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", new Locale("pt", "BR"));
        @Override public int getCount() { return visibleCalls.size(); }
        @Override public Object getItem(int position) { return visibleCalls.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View reused, ViewGroup parent) {
            RowHolder holder;
            LinearLayout row;
            if (reused == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(13), dp(12), dp(13));
                row.setBackground(rounded(Color.WHITE, 14, Color.rgb(231, 237, 233)));

                LinearLayout info = new LinearLayout(MainActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1f);
                infoLp.rightMargin = dp(6);
                row.addView(info, infoLp);

                TextView name = makeText("", 15, DARK, true);
                name.setSingleLine(true);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                info.addView(name);

                TextView number = makeText("", 13, MUTED, false);
                number.setSingleLine(true);
                number.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams numberLp = new LinearLayout.LayoutParams(-1, -2);
                numberLp.topMargin = dp(3);
                info.addView(number, numberLp);

                TextView meta = makeText("", 12, DARK, true);
                LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2);
                metaLp.topMargin = dp(5);
                info.addView(meta, metaLp);

                TextView timestamp = makeText("", 11, MUTED, false);
                LinearLayout.LayoutParams timestampLp = new LinearLayout.LayoutParams(-1, -2);
                timestampLp.topMargin = dp(5);
                info.addView(timestamp, timestampLp);

                Button whatsapp = smallButton("WhatsApp", true);
                whatsapp.setTextSize(12);
                whatsapp.setPadding(dp(8), 0, dp(8), 0);
                row.addView(whatsapp, new LinearLayout.LayoutParams(dp(105), dp(42)));

                holder = new RowHolder(name, number, meta, timestamp, whatsapp);
                row.setTag(holder);
            } else {
                row = (LinearLayout) reused;
                holder = (RowHolder) row.getTag();
            }
            CallItem item = visibleCalls.get(position);
            holder.name.setText(item.name == null || item.name.trim().isEmpty()
                    ? "Número não salvo" : item.name);
            holder.number.setText(item.number);
            holder.meta.setText(callStatus(item));
            holder.meta.setTextColor(item.type == CallLog.Calls.MISSED_TYPE
                    || item.type == CallLog.Calls.REJECTED_TYPE ? RED : DARK);
            holder.timestamp.setText(dateFormat.format(new Date(item.when)));
            holder.whatsapp.setOnClickListener(v -> openWhatsApp(item.number));
            holder.whatsapp.setOnLongClickListener(v -> { promptForNumber(item.number); return true; });
            return row;
        }

        /**
         * Direção e atendimento são baseados nos tipos fornecidos pelo Android.
         * Para saída, DURATION não é confirmação oficial de atendimento.
         */
        private String callStatus(CallItem item) {
            if (item.type == CallLog.Calls.INCOMING_TYPE) {
                return "Ligaram para você • Atendida • " + durationText(item.durationSeconds);
            }
            if (item.type == CallLog.Calls.MISSED_TYPE) {
                return "Ligaram para você • Não atendida";
            }
            if (item.type == CallLog.Calls.REJECTED_TYPE) {
                return "Ligaram para você • Recusada";
            }
            if (item.type == CallLog.Calls.BLOCKED_TYPE) {
                return "Ligaram para você • Bloqueada";
            }
            if (item.type == CallLog.Calls.OUTGOING_TYPE) {
                return item.durationSeconds > 0
                    ? "Você ligou • " + durationText(item.durationSeconds)
                    : "Você ligou • Atendimento não confirmado";
            }
            if (item.type == CallLog.Calls.ANSWERED_EXTERNALLY_TYPE) {
                return "Ligaram para você • Atendida em outro aparelho";
            }
            if (item.type == CallLog.Calls.VOICEMAIL_TYPE) {
                return "Ligaram para você • Correio de voz";
            }
            return "Ligação • Situação não informada";
        }

        private String durationText(long seconds) {
            if (seconds < 60) return "Duração: " + seconds + " s";
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long remainder = seconds % 60;
            if (hours > 0) return "Duração: " + hours + " h " + minutes + " min";
            return "Duração: " + minutes + " min " + remainder + " s";
        }

    }

    private static class RowHolder {
        final TextView name, number, meta, timestamp;
        final Button whatsapp;
        RowHolder(TextView name, TextView number, TextView meta, TextView timestamp, Button whatsapp) {
            this.name = name;
            this.number = number;
            this.meta = meta;
            this.timestamp = timestamp;
            this.whatsapp = whatsapp;
        }
    }
}
