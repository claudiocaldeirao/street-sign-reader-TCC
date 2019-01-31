package com.claudiocaldeirao.platereader.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

/**
 * Controlador da API OCR Google Cloud Vision.
 *
 * Created by claudiocaldeirao on 02/04/2018.
 */
public class GoogleCloudVisionManager {
    private static String TAG = "Google Cloud Vision";
    private static String TAG2 = "Google_Cloud_Vision_Time_Elapsed";

    /**
     * Metodo que tenta reconhecer os caracteres contidos em uma imagem do tipo bitmap
     * Recebe como parametro uma imagem e tem como retorno uma string.
     * @param image
     * @return
     */
    public String startReconizer(Bitmap image, Context context) {
        //Medindo o tempo de execução do processo em millisegundos.
        long start = System.currentTimeMillis();

        String result = null;
        TextRecognizer recognizer = new TextRecognizer.Builder(context).build();

        if(recognizer.isOperational()) {
            Log.d(TAG, "Google Cloud Vision está operando normalmente.");
            //Frame da imagem que será reconhecida.
            Frame frame = new Frame.Builder().setBitmap(image).build();
            //Detecta a string na imagem.
            SparseArray<TextBlock> itens = recognizer.detect(frame);
            //Classe que constroi a string apartir do TextBlock.
            StringBuilder stringBuilder = new StringBuilder();
            //Montando a string.
            for (int i = 0; i < itens.size(); i++) {
                TextBlock item = itens.valueAt(i);
                stringBuilder.append(item.getValue());
                stringBuilder.append("\n");
            }
            result = stringBuilder.toString();
            //Computa o tempo de execução do processo.
            long elapsed = System.currentTimeMillis() - start;
            Log.d(TAG2, String.valueOf(elapsed) + " milisegundos.");
            //Retornando o resultado.
            return result;
        } else {
            Log.d(TAG, "Google Cloud Vision não está operando.");
            return null;
        }
    }
}
