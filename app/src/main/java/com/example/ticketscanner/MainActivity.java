package com.example.ticketscanner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.widget.Button;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONObject;

public class   MainActivity extends AppCompatActivity {

    Button btn_scan;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_scan = findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(v -> {
            scanCode();
        });
    }

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume UP to flash ON");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }
    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null){
            String s = result.getContents();
            String ticketid = s.substring(0,2);
            String name = s.substring(2,s.length()-4);
            String name1 = name.toUpperCase();
            String time = s.substring(s.length()-4);
            int j = Integer.parseInt(time);
            int ticket_id = Integer.parseInt(ticketid);

            Date dateAndTime = Calendar.getInstance().getTime();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.getDefault());
            String text = timeFormat.format(dateAndTime);
            int i = Integer.parseInt(text);
            checkTicketOnServer(ticket_id);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Result");
            if(i==j || i==j+1){
                String sms1 = "PASSED"+'\n'+'\n'+"WELCOME, "+name1;
                SpannableString ss = new SpannableString(sms1);
                ForegroundColorSpan fcsGreen = new ForegroundColorSpan(Color.GREEN);
                ss.setSpan(fcsGreen, 0, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setMessage(ss);
            }
            else
            {
                String sms2 = "REJECTED"+'\n'+'\n'+"SORRY, "+name1+'\n'+"Please, try again !";
                SpannableString sss = new SpannableString(sms2);
                ForegroundColorSpan fcsRed = new ForegroundColorSpan(Color.RED);
                sss.setSpan(fcsRed, 0, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setMessage(sss);
            }
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
        }
    });

    private void checkTicketOnServer(int ticket_id) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://192.168.0.114/Anti%20Black%20Ticket%20System/Anti%20Black%20Ticket%20System/api/get_ticket.php?ticket_id=" + ticket_id;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    // Show error dialog
                    showResultDialog("Error", "Failed to connect to server."+url);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        if (json.has("error")) {
                            showResultDialog("Invalid Ticket", "Ticket not found in database.");
                        } else {
                            // Optionally extract name from the JSON if your DB has it
                            String name = json.optString("name", "Unknown");
                            showResultDialog("Valid Ticket","Valid Ticket");
                        }
                    } catch (Exception e) {
                        showResultDialog("Error", "Invalid server response.");
                    }
                });
            }
        });
    }

    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
