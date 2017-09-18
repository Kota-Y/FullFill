package io.haruya.fullfill;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by hayato on 2016/12/12.
 */

public class Registration extends Activity {

    String select_http_user = "http://fullfill.sakura.ne.jp/src/userSelect.php";
    String send_http_profile = "http://fullfill.sakura.ne.jp/src/profileInsert.php";

    private LinearLayout inflateLayout;
    InputStream is;


    ArrayList<NameValuePair> nameValue_send_p = new ArrayList<NameValuePair>();
    ArrayList<NameValuePair> nameValue_select_u = new ArrayList<NameValuePair>();

    HttpPost httppost = new HttpPost(send_http_profile);


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        TextView to_terms = (TextView)findViewById(R.id.to_terms_text);
        to_terms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), Terms_of_service.class);
                startActivity(intent);
            }
        });

        register();
    }

    private void register() {

        //登録ボタンを押したときの処理
        Button button = (Button) findViewById(R.id.regiButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean flag = true;//falseの時に遷移せずに注意文を表示するためのflag


                // EditTextオブジェクトを取得
                EditText edit_name = (EditText) findViewById(R.id.edit_name);
                EditText edit_id = (EditText) findViewById(R.id.edit_id);
                EditText edit_pw = (EditText) findViewById(R.id.edit_pw);
                EditText edit_re_pw = (EditText) findViewById(R.id.edit_re_pw);

                // 入力された文字を取得
                String text_name = edit_name.getText().toString();
                String text_id = edit_id.getText().toString();
                String text_pw = edit_pw.getText().toString();
                String text_re_pw = edit_re_pw.getText().toString();

                //利用規約のチェックボックスのViewとチェックのON/OFFの状態を取得
                CheckBox read_terms = (CheckBox)findViewById(R.id.terms_check);
                boolean check_or_not = read_terms.isChecked();
                if(check_or_not==false){
                    make_attention("利用規約を確認の上、上記のチェックボックスにチェックしてください");
                    flag = false;
                }


                // 入力内容の正否をチェック
                if (text_name.length() == 0 || text_id.length() == 0 || text_pw.length() == 0 || text_re_pw.length() == 0){
                    make_attention("未記入の項目があります。");
                    flag = false;
                }

                if(text_name.length() > 20) {
                    make_attention("ニックネームの文字数が不正です。20字以内にしてください。");
                    flag = false;
                }

                if(text_id.length() > 20) {
                    make_attention("IDの文字数が不正です。20字以内にしてください。");
                    flag = false;
                } else if( !text_id.matches("[0-9a-zA-Z_]+") && text_id.length() != 0) {
                    make_attention("IDには英数字と_以外は使用できません。");
                    flag = false;
                } else if(id_duplicate_check(nameValue_select_u, text_id)){
                    flag = false;
                }

                if (text_pw.length() == 0 || text_re_pw.length() == 0) {
                } else if (!(text_pw.equals(text_re_pw))) {
                    make_attention("パスワードが一致しません。もう一度入力してください。");
                    flag = false;
                } else if(text_pw.length() < 8 || text_pw.length() > 20) {
                    make_attention("パスワードの文字数が不正です。8-20字にしてください。");
                    flag = false;
                }


                // 登録内容をpostする
                if (flag == true) {
                    try {
                        nameValue_send_p.add(new BasicNameValuePair("username", text_name));
                        nameValue_send_p.add(new BasicNameValuePair("userid", text_id));
                        nameValue_send_p.add(new BasicNameValuePair("password", text_pw));

                        HttpClient httpclient = new DefaultHttpClient(null);
                        HttpPost httppost = new HttpPost(send_http_profile);
                        httppost.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=UTF-8");
                        httppost.setEntity(new UrlEncodedFormEntity(nameValue_send_p, "UJIS"));
                        HttpResponse response = httpclient.execute(httppost);


                        HttpEntity entity = response.getEntity();

                        // ------------ここで遷移させるように後で変更--------------
                        make_attention("登録が完了しました。\n3秒後にログイン画面に移動します。");

                        // 1200ms遅延させてsplashHandlerを実行します。
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 3000);
                    } catch (Exception e) {
                        Log.e("log_tag", "Error converting result " + e.toString());
                    }
                }
            }
        });
    }

    // 注意文の作成
    private void make_attention(String word){
        TextView attention = (TextView)findViewById(R.id.attention);
        attention.setText(word);
    }

    // ユーザIDのダブりチェック
    private boolean id_duplicate_check(final ArrayList list_select_u, String text_id) {

        try {
            String result_get_p = "";

            try {
                HttpClient httpclient = new DefaultHttpClient(null);
                HttpPost p_httppost = new HttpPost(select_http_user);
                p_httppost.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=UTF-8");
                p_httppost.setEntity(new UrlEncodedFormEntity(list_select_u, "UJIS"));
                HttpResponse response = httpclient.execute(p_httppost);

                HttpEntity entity = response.getEntity();
                is = entity.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();

                result_get_p = sb.toString();
            } catch (Exception e) {
                Log.e("log_tag", "Error converting result " + e.toString());
            }

            JSONArray jArray_p = new JSONArray(result_get_p);
            String[] any_users_id = new String[jArray_p.length()];

            for (int i = 0; i < jArray_p.length(); i++) {
                JSONObject json_data_get_p = jArray_p.getJSONObject(i);
                any_users_id[i] = json_data_get_p.getString("userid");
                if(any_users_id[i].equals(text_id)){
                    make_attention("そのIDは使われています。");
                    return true;
                }
            }
        } catch (JSONException e) {
            Log.e("log_tag", "Error parsing data " + e.toString());
        }
        return false;

    }


}
