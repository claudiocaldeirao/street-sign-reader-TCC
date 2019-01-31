package com.claudiocaldeirao.platereader.managers;

import android.graphics.Bitmap;
import android.util.Log;
import com.claudiocaldeirao.platereader.traineddata.LoadTrainedData;
import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Controlador da API Tesseract.
 *
 * Created by claudiocaldeirao on 18/10/2017.
 */
public class TesseractManager {
    private static final String TAG = "Tesseract_Time_Elapsed";

    private TessBaseAPI baseAPI = null;

    /**
    * Metodo que inicializa a API
    */
    public void initAPI() {
        baseAPI = new TessBaseAPI();
        //Path da traineddata.
        String datapath = LoadTrainedData.instance.getTessDataParentDirectory();
        //Inicializando API.
        baseAPI.init(datapath, "por");
    }

    /**
     * Metodo que tenta reconhecer os caracteres contidos em uma imagem do tipo bitmap
     * Recebe como parametro uma imagem e tem como retorno uma string.
     * @param image
     * @return
     */
    public String startReconizer(Bitmap image) {
        if(baseAPI == null) {
            this.initAPI();
        }
        baseAPI.setImage(image);
        //Medindo o tempo de execução do processo em millisegundos.
        long start = System.currentTimeMillis();
        //Verificando a imagem.
        String result = baseAPI.getUTF8Text();
        //Computa o tempo de execução do processo.
        long elapsed = System.currentTimeMillis() - start;
        Log.d(TAG, String.valueOf(elapsed) + " milisegundos.");
        //Retornando o resultado.
        return result;
    }
}
