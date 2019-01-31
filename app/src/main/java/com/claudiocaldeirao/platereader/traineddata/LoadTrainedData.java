package com.claudiocaldeirao.platereader.traineddata;

import android.app.Application;
import android.content.res.AssetManager;
import android.util.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Clase que faz com que a traineddata seja carregada juntamente com a atividade quando a aplicacao se inicia.
 *
 * Created by claudiocaldeirao on 18/10/2017.
 */

public class LoadTrainedData extends Application {
    public static LoadTrainedData instance = null;

    /**
     * Metodo invocado quando a aplicacao esta iniciando
     */
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.copyTessDataForTextRecognizer();
    }

    /**
     * Metodo que retorna o diretorio (path) da traineddata no diretorio aplicacao
     */
    private String tessDataPath() {
        return LoadTrainedData.instance.getExternalFilesDir(null)+"/tessdata/";
    }

    /**
     * Retorna o caminho absoluto da traineddata para o TesseractManager
     */
    public String getTessDataParentDirectory() {
        return LoadTrainedData.instance.getExternalFilesDir(null).getAbsolutePath();
    }

    /**
     * Copia a traineddata para o diretorio da aplicacao no Android
     */
    private void copyTessDataForTextRecognizer() {

        Runnable run = new Runnable() {
            @Override
            public void run() {
                AssetManager assetManager = LoadTrainedData.instance.getAssets();
                OutputStream out =null;
                try {
                    InputStream in = assetManager.open("por.traineddata");
                    String tesspath = instance.tessDataPath();
                    File tessFolder = new File(tesspath);
                    if(!tessFolder.exists())
                        tessFolder.mkdir();
                    String tessData = tesspath+"/"+"por.traineddata";
                    File tessFile = new File(tessData);
                    if(!tessFile.exists())
                    {
                        out = new FileOutputStream(tessData);
                        byte[] buffer = new byte[1024];
                        int read = in.read(buffer);
                        while (read != -1) {
                            out.write(buffer, 0, read);
                            read = in.read(buffer);
                        }
                        Log.d("LoadTrainedData", " Did finish copy tess file  ");
                    }
                    else
                        Log.d("LoadTrainedData", " tess file exist  ");
                } catch (Exception e)
                {
                    Log.d("LoadTrainedData", "couldn't copy with the following error : "+e.toString());
                }finally {
                    try {
                        if(out!=null)
                            out.close();
                    }catch (Exception exx) {

                    }
                }
            }
        };
        new Thread(run).start();
    }
}
