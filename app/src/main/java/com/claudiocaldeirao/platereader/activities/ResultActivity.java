package com.claudiocaldeirao.platereader.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.claudiocaldeirao.platereader.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import java.io.ByteArrayInputStream;

/**
 * Sub-atividade para exibir o resultado do reconhecimento de caracteres.
 * imageView: imagem após a etapa de pré-processamento de imagem.
 * textView: string extraída da imagem pelo tesseract.
 *
 * Created by claudiocaldeirao on 13/01/2018.
 */
public class ResultActivity extends Activity {
    //Consantes.
    private static final String TAG = "MapActivity";
    public static final String TEXT_MESSAGE = "text_message";
    public static final String RADIUS_SIZE = "radius_size";
    //Elementos da interface.
    private ImageView imageView;
    private TextView textView;
    private Spinner radiusSpinner;

    /**
     * Método invocado quando a atividade é criada.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_view);
        //Recupera o intent da atividade pai.
        Intent intent = getIntent();
        //Recuperando a string resultado do intent e exibindo no textview.
        String result = intent.getStringExtra(MainActivity.RESULTED_TEXT);
        //Vincula a variável ao componente na interface.
        textView = (TextView) findViewById(R.id.text_view);
        //Seta o conteúdo.
        textView.setText(result);
        //Recuperando a imagem pós processamento do intent e exibindo no imageview.
        byte[] arrayByte = intent.getByteArrayExtra(MainActivity.RESULTED_IMAGE);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(arrayByte);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        //Vincula a variável ao componente na interface.
        imageView = (ImageView) findViewById(R.id.image_view);
        //Seta o conteúdo.
        imageView.setImageBitmap(bitmap);
        //Setando o Spinner.
        radiusSpinner = (Spinner) findViewById(R.id.radius_spinner);
        //Criando o adapter com os valores e layout do radiusSpinner.
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.radius_size, android.R.layout.simple_spinner_item);
        //Setando o adapter no componente.
        radiusSpinner.setAdapter(adapter);
        //Verifica a disponibilidade dos serviçõs do google services.
        if (isServicesOK()) {
            //Inicializa o botão do mapa.
            init();
        }
    }

    /**
     * Inicia o botão do mapa se tudo estiver OK com o google services.
     */
    public void init() {
        Button mapbtn = (Button) findViewById(R.id.map_btn);
        mapbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Adiciona a string à intent para ser repassada à MapsActivity.
                Intent intent = new Intent(ResultActivity.this, MapsActivity.class);
                intent.putExtra(TEXT_MESSAGE, textView.getText());
                //Adiciona o parametro raio ao intent.
                String radius = radiusSpinner.getSelectedItem().toString();
                //Removendo a sigla "Km".
                radius = radius.substring(0, radius.length() - 2);
                intent.putExtra(RADIUS_SIZE, radius);
                //Inicia a MapsActivity.
                startActivity(intent);
            }
        });
    }

    /**
     * Método apenas para verificar se podemos acessar os serviçõs do google maps.
     */
    public boolean isServicesOK() {
        Log.d(TAG, "Checando a versão do Google Services...");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ResultActivity.this);

        if(available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Services está funcionando normalmente!");
            return true;
        } else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.d(TAG, "Um erro ocorreu, mas é possivel resolve-lo!");
        } else {
            Toast.makeText(this, "Não é possivel fazer requisições ao Google Maps!", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
