package com.claudiocaldeirao.platereader.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.claudiocaldeirao.platereader.R;
import com.claudiocaldeirao.platereader.managers.OpenCvManager;

import org.opencv.android.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Atividade que permite que o usuário altere o brilho e o contraste da imagem.
 *
 * Created by claudiocaldeirao on 18/04/2018.
 */
public class BrightnessAndContrastActivity extends Activity {
    //Constantes.
    public static  final String BC_TAG = "modified_image";
    //Controladores.
    private OpenCvManager opencvManager;
    //Componentes da interface.
    private SeekBar brightnessBar;
    private SeekBar contrastBar;
    private Button finish_btn;
    private ImageView imageView;
    //Variaveis.
    private int contrast;
    private int brightness;
    private Bitmap bitmap;
    private Bitmap processedBitmap;

    /**
     * Método invocado quando a atividade é criada.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bringhtness_and_contrast);
        //Recupera o intent da atividade pai.
        Intent intent = getIntent();
        String path = intent.getStringExtra(MainActivity.RESULTED_IMAGE);
        //Caminho da imagem.
        Uri imageUri = Uri.parse(path);
        //Imagem view.
        imageView = (ImageView) findViewById(R.id.image_view);
        //Recuperando a imagem serializada no passo anterior.
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            //Constroi a bitmap.
            bitmap = BitmapFactory.decodeStream(inputStream);
            //Seta a bitmap na view.
            imageView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();    //ERRO NO ARQUIVO!
            Toast.makeText(this, "Erro ao carregar imagem", Toast.LENGTH_LONG).show();
        }
        //Inicializando o controlador do OpenCV.
        opencvManager = new OpenCvManager();
        //Slider de brilho.
        brightnessBar = (SeekBar) findViewById(R.id.brightness_bar);
        //Valor maximo possivel de brilho.
        brightnessBar.setMax(100);
        //O centro da barra é o ponto "nulo", onde não altera o brilho da imagem original.
        brightness = 50;
        brightnessBar.setProgress(brightness);
        brightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //Setando o valor da seekbar na variavel brightness.
                brightness = i;
                //Aplicando o método na imagem.
                processedBitmap = opencvManager.setContrastAndBrightness(bitmap, contrast, brightness);
                //Setando a imagem na view.
                imageView.setImageBitmap(processedBitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        //Slider de contraste.
        contrastBar = (SeekBar) findViewById(R.id.contrast_bar);
        //Valor maximo possivel de contraste.
        contrastBar.setMax(100);
        //O centro da barra é o ponto "nulo", onde não altera o contraste da imagem original.
        contrast = 50;
        contrastBar.setProgress(contrast);
        contrastBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //Setando o valor da seekbar na variavel contrast.
                contrast = i;
                //Aplicando o método na imagem.
                processedBitmap = opencvManager.setContrastAndBrightness(bitmap, contrast, brightness);
                //Setando a imagem na view.
                imageView.setImageBitmap(processedBitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        //Botão que finaliza o proceso e retorna a imagem modificada à atividade principal.
        finish_btn = (Button) findViewById(R.id.finish_btn);
        finish_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnModifiedImage();
            }
        });
    }

    /**
     * Retorna a imagem modificada para a atividade principal.
     */
    private void returnModifiedImage() {
        Intent intent = new Intent();
        //Stream utilizado para transformar o bitmap em um array de bytes.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //Carregamos a imagem em um array de bytes para conseguirmos envia-la por intent.
        if(processedBitmap == null) {
            processedBitmap = bitmap;
        }
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        try {
            intent.putExtra(BC_TAG, outputStream.toByteArray());
            setResult(RESULT_OK, intent);
        } catch (Exception e){
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
