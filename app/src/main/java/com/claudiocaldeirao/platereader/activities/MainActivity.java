package com.claudiocaldeirao.platereader.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;
import com.claudiocaldeirao.platereader.managers.GoogleCloudVisionManager;
import com.claudiocaldeirao.platereader.managers.OpenCvManager;
import com.claudiocaldeirao.platereader.managers.TesseractManager;
import com.claudiocaldeirao.platereader.R;
import com.yalantis.ucrop.UCrop;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Atividade principal do aplicativo, permite ao usuário capturar uma imagem através da camera do dispositivo ou
 * carregar uma imagem da galeria do celular.
 *
 * Created by claudiocaldeirao on 18/10/2017.
 */
public class MainActivity extends Activity {
    //Constantes.
    public static final String RESULTED_TEXT = "resulted_text";
    public static final String RESULTED_IMAGE = "resulted_image";
    private static final String TAG = "MainActivity";
    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_FILE = 2;
    public static final int BRIGHT_CONST_IMG_REQUEST = 3;
    //Controladores.
    private GoogleCloudVisionManager googleCloudVisionManager;
    private TesseractManager tesseractManager;
    private OpenCvManager opencvManager;
    //Componentes da interface.
    private RelativeLayout mLayout;
    private ImageView imageView;
    private Button galleryBtn;
    private Button cameraBtn;
    private Button cprBtn;
    private Button brcBtn;
    private Button perspectiveCorrectionBtn;
    private Button ocrBtn;
    private Spinner ocrSpinner;
    //Variaveis.
    private Uri imageCapturedUri;
    private Bitmap bitmap;
    private Mat originalMat;
    private int coordsCount;
    private ArrayList<Point> coords;
    private MatOfPoint boundingBox;

    /**
     * Método invocado quando a atividade é criada.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Inicializando o controlador do Tesseract OCR.
        tesseractManager = new TesseractManager();
        tesseractManager.initAPI();
        //Inicializando o controlador do OpenCV.
        opencvManager = new OpenCvManager();
        //Inicializando o controlador do Google Cloud Vision.
        googleCloudVisionManager = new GoogleCloudVisionManager();
        //Spinner que seleciona a API OCR.
        ocrSpinner = (Spinner) findViewById(R.id.ocr_spinner);
        //Criando o adapter com os valores e layout do radiusSpinner.
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.OCR_API, android.R.layout.simple_spinner_item);
        //Setando o adapter no componente.
        ocrSpinner.setAdapter(adapter);
        //Botão da galeria:
        galleryBtn = (Button) findViewById(R.id.gallery_btn);
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Carregando a imagem a partir do armazenamento interno.
                loadImageFromStorage();
            }
        });
        //Botão da camera.
        cameraBtn = (Button) findViewById(R.id.photo_btn);
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Carregando a imagem a partir da camera.
                loadImageFromCamera();
            }
        });
        //Botão do cropper.
        cprBtn = (Button) findViewById(R.id.crop_btn);
        cprBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImage();
            }
        });
        //Botão da correção geométrica.
        perspectiveCorrectionBtn = (Button) findViewById(R.id.perscorrect_btn);
        perspectiveCorrectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Invocando o método de correção geométrica.
                perspectiveCorrection();
            }
        });
        //Botão para setar o brilho e o contraste da imagem.
        brcBtn = (Button) findViewById(R.id.bright_cont_btn);
        brcBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Invocando o método para alterar brilho e contraste.
                setBrightnessAndContrast();
            }
        });
        //Botão do reconhecedor.
        ocrBtn = (Button) findViewById(R.id.recognize_btn);
        ocrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recognize();
            }
        });
        //View da imagem selecionada.
        imageView = (ImageView) findViewById(R.id.image_view);
        //Configurando o evento "on touch, para interarir com a view e desenhar a boundingbox.
        mLayout = (RelativeLayout) findViewById(R.id.relative_layout);
        mLayout.setOnTouchListener(new View.OnTouchListener() {
            private Matrix matInverse = new Matrix();
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if ((boundingBox == null) || (coords == null)) {
                    coords = new ArrayList<>();
                    boundingBox = new MatOfPoint();
                    coordsCount = 0;
                }

                switch (motionEvent.getActionMasked()) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        return true;
                    case MotionEvent.ACTION_DOWN:
                        //Pegando coordenadas da view.
                        float[] pts = {motionEvent.getX(), motionEvent.getY()};
                        //Se a bitmap não for nula calcula as coordenadas reais na imagem.
                        if (bitmap != null) {
                            //Usaremos apenas quatro pontos para realizar o corte/correção geométrica.
                            if(coordsCount < 3) {
                                imageView.getImageMatrix().invert(matInverse);
                                matInverse.mapPoints(pts);
                                //Coordenadas reais na imagem.
                                double x = Math.floor(pts[0]);
                                double y = Math.floor(pts[1]);
                                coords.add(new Point(x, y));
                                coordsCount++;
                            } else {
                                //Quarto Point da boundingBox.
                                if (coordsCount == 3) {
                                    imageView.getImageMatrix().invert(matInverse);
                                    matInverse.mapPoints(pts);
                                    //Coordenadas reais na imagem.
                                    double x = Math.floor(pts[0]);
                                    double y = Math.floor(pts[1]);
                                    coords.add(new Point(x, y));
                                    coordsCount++;
                                } else {
                                    //Substitui o Point mais proximo da nova coordenada.
                                    imageView.getImageMatrix().invert(matInverse);
                                    matInverse.mapPoints(pts);
                                    //Coordenadas reais na imagem.
                                    double x = Math.floor(pts[0]);
                                    double y = Math.floor(pts[1]);
                                    Point newPoint = new Point(x, y);
                                    Point nearest = nearestPoint(newPoint);
                                    //Se houver algum Point mais próximo.
                                    if (nearest != null) {
                                        //Remove o ponto mais proximo.
                                        int i = coords.indexOf(nearest);
                                        //Adiciona um novo ponto.
                                        coords.add(i, newPoint);
                                        coords.remove(nearest);
                                        //Logo após, remonta a boundingbox.
                                    }
                                }
                                //Criando uma MatOfPoint a partir do array.
                                boundingBox.fromList(coords);
                                //Convertendo o bitmap em mat.
                                Mat img;
                                //Preserva a matriz original (para não encher a view de contornos).
                                if (originalMat == null) {
                                    originalMat = new Mat();
                                    Utils.bitmapToMat(bitmap, originalMat);
                                    img = originalMat.clone();
                                } else {
                                    img = originalMat.clone();
                                }
                                //Lista de boundingboxes (mas nesse caso utilizaremos apenas uma).
                                ArrayList<MatOfPoint> boundingBoxes = new ArrayList<>();
                                //Adicionando o boudingbox à lista.
                                boundingBoxes.add(boundingBox);
                                //Desenhando o poligono para sinalizar o contorno.
                                Core.polylines(img, boundingBoxes, true, new Scalar(0,255,0), 8);
                                //Converte a mat para bitmap.
                                Bitmap newBitmap = Bitmap.createBitmap(img.width(), img.height(), Bitmap.Config.ARGB_8888);
                                Utils.matToBitmap(img, newBitmap);
                                //Setando a bitmap no atributo do objeto.
                                bitmap = newBitmap;
                                //Seta a bitmap na view.
                                imageView.setImageBitmap(bitmap);
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Método responsável por retornar o ponto mais próximo do ponto passado como parametro.
     * Percorre o vetor de pontos (arestas da boundingbox), e retorna o ponto mais proximo de point.
     *
     * @param point
     * @return
     */
    private Point nearestPoint(Point point) {
        Point nearest = null;
        //Distancia no eixo x (dx^2) do pointo nearest ao ponto Point.
        double dx = 0;
        //Distancia no eixo y (dy^2) do pointo nearest ao ponto Point.
        double dy = 0;

        for (Point coord: coords) {
            if(nearest == null) {
                nearest = coord;
                //Distancia no eixo x.
                dx = Math.pow(point.x - coord.x, 2);
                //Distancia no eixo y.
                dy = Math.pow(point.y - coord.y, 2);
            } else {
                //Distancia no eixo x do ponto coord ao ponto point.
                double dx1 = Math.pow(point.x - coord.x, 2);
                //Distancia no eixo y do ponto coord ao ponto point.
                double dy1 = Math.pow(point.y - coord.y, 2);
                //Se a distancia do coord ao point for menor que a distancia do nearest, substitumos o nearest por coord.
                if(dx1 + dy1 <  dx + dy) {
                    nearest = coord;
                    dx = dx1;
                    dy = dy1;
                }
            }
        }

        return nearest;
    }

    /**
     * Método invocado quando a camera ou file_chooser retorna para a atividade principal.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //
        if(resultCode == RESULT_OK) {
            //Resetando a boundingbox.
            coords = null;
            boundingBox = null;
            coordsCount = 0;
            originalMat = null;
            //Opção de escolher imagem a partir da galeria selecionada.
            if(requestCode == PICK_FROM_FILE) {
                //Uri da imagem.
                imageCapturedUri = data.getData();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageCapturedUri);
                    //Constroi a bitmap.
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    //Seta a bitmap na view.
                    imageView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();    //ERRO NO ARQUIVO!
                    Toast.makeText(this, "Erro ao carregar imagem", Toast.LENGTH_LONG).show();
                }
            //Opção de escolher imagem a partir da camera.
            } else if(requestCode == PICK_FROM_CAMERA)  {
                //Pega o caminho da imagem capturada.
                String path = imageCapturedUri.getPath();
                //Constroi a bitmap.
                bitmap = BitmapFactory.decodeFile(path);
                //Seta a bitmap na view.
                imageView.setImageBitmap(bitmap);
            } else if (requestCode == UCrop.REQUEST_CROP) {
                final Uri resultUri = UCrop.getOutput(data);
                imageCapturedUri = resultUri;
                //Pega o caminho da imagem capturada.
                String path = resultUri.getPath();
                //Constroi a bitmap.
                bitmap = BitmapFactory.decodeFile(path);
                //Seta a bitmap na view.
                imageView.setImageBitmap(bitmap);
            } else if (resultCode == UCrop.RESULT_ERROR) {
                final Throwable cropError = UCrop.getError(data);
            //Opção de alteração no brilho e contraste da imagem.
            } else if(requestCode == BRIGHT_CONST_IMG_REQUEST) {
                Log.d(TAG, "Sucesso ao retornar imagem com contraste e brilho alterados.");
                //Recuperando a imagem pós processamento do intent e exibindo no imageview.
                byte[] arrayByte = data.getByteArrayExtra(BrightnessAndContrastActivity.BC_TAG);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(arrayByte);
                bitmap = BitmapFactory.decodeStream(inputStream);
                //Seta o conteúdo.
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    /**
     * Método invocado quando a atividade está em segundo plano.
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Método invocado quando a atividade é encerrada.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Método invocado quando a atividade está em primeiro plano.
     */
    @Override
    protected void onResume() {
        super.onResume();
        //Apenas para verificar se a biblioteca opencv foi carregada com sucesso.
        if(OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV carregado com sucesso!");
        } else {
            Log.d(TAG, "Erro ao carregar OpenCV!");
        }
    }

    //***************
    //AÇÃO DOS BOTÕES
    //***************

    /**
     * Método que carrega a imagem a partir do armazenamento interno.
     */
    private void loadImageFromStorage() {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                Intent imgIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(imgIntent, PICK_FROM_FILE);
                //File chooser antigo (não permitia ver a miniatura da imagem).
                //imgIntent.setType("image/*");
                //imgIntent.setAction(Intent.ACTION_GET_CONTENT);
                //startActivityForResult(Intent.createChooser(imgIntent, "Selecionar Imagem"), PICK_FROM_FILE);
            }
        };
        new Thread(run).start();
    }

    /**
     * Método que carrega a imagem a partir da camera.
     */
    private void loadImageFromCamera() {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                Intent imgIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File imgFile = new File(Environment.getExternalStorageDirectory() + "/Prototype/temporary/", "tmp_img" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                imageCapturedUri = Uri.fromFile(imgFile);
                try {
                    imgIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageCapturedUri);
                    imgIntent.putExtra("return data", true);
                    startActivityForResult(imgIntent, PICK_FROM_CAMERA);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(run).start();
    }

    /**
     * Método que carrega recorta a imagem.
     * (Passa o controle pra atividade que permite ao usuário cortar a imagem).
     */
    private void cropImage() {
        File temp = new File(getCacheDir(), "temp.png");
        Uri destinationUri = Uri.fromFile(temp);

        UCrop.Options options = new UCrop.Options();
        options.setFreeStyleCropEnabled(true);
        options.setActiveWidgetColor(30);

        UCrop.of(imageCapturedUri, destinationUri)
                .withOptions(options)
                .start(this);
    }

    /**
     * Método que invoca a atividade que permite que o usuário altere o contraste e o brilho da imagem.
     */
    private void setBrightnessAndContrast() {
        //Intent da BrightAndContrastActivty que usaremos para alterar o contraste e brilho da imagem.
        Intent intent = new Intent(this, BrightnessAndContrastActivity.class);
        //Chamando o método que serializa o bitmap, para gerar uma URI que será transferida para a sub-activity.
        //Esse passo é nescessário pois se a imagem for muito grande não tem como ser enviada diretamente,
        //então enviamos o caminho da mesma.
        Uri imageUri = getImageUri(bitmap, Bitmap.CompressFormat.JPEG, 100);
        intent.putExtra(RESULTED_IMAGE, imageUri.toString());
        //Inicia a ResultActivity.
        startActivityForResult(intent, BRIGHT_CONST_IMG_REQUEST);
    }

    /**
     * Método que invoca o procedimento em opencv que efetua a correção geométrica da imagem,
     * usando como base as quatro arestas da boundingbox.
     */
    private void perspectiveCorrection() {
        if(coords != null) {
            bitmap = opencvManager.PerspectiveCorrection(bitmap, coords);
            imageView.setImageBitmap(bitmap);
        }
    }

    /**
     * Método que tenta extraír o texto da imagem.
     * Primeiro efetua uma série de pré-processamentos de imagem utilizando o opencv e em seguida
     * a imagem processada é passada como parametro para a engine do tesseract, que extraí o texto da imagem.
     */
    private void recognize() {
        //String resultado.
        String result;
        //Stream utilizado para transformar o bitmap em um array de bytes.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //Intent da ResultActivity, que usaremos para exibir a imagem processada e a string obtida.
        Intent intent = new Intent(this, ResultActivity.class);
        //Para garantir que a aplicação não tente reconhecer imagens nulas.
        if(bitmap != null) {
            //Pré processamento da imagem.
            //processedBitmap recebe a imagem após os processamentos realizados com opencv.
            Bitmap processedBitmap = opencvManager.processImage(bitmap);
            //Verifica qual OCR foi selecionada.
            String ocrSelected = ocrSpinner.getSelectedItem().toString();
            switch (ocrSelected) {
                case "Tesseract OCR":
                    //Reconhecendo os caracteres.
                    //A imagem processada é passada como parametro para o tesseract, que retorna uma string do texto abstraido da imagem.
                    result = tesseractManager.startReconizer(processedBitmap);
                    //Armazena o resultado no intent para ser repassado para a ResultActivity.
                    intent.putExtra(RESULTED_TEXT, result);
                    //Carregamos a imagem em um array de bytes para conseguirmos envia-la por intent.
                    processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    intent.putExtra(RESULTED_IMAGE, outputStream.toByteArray());
                    //Inicia a ResultActivity.
                    startActivity(intent);
                    break;
                case "Google Cloud Vision API":
                    //Reconhecendo os caracteres.
                    //A imagem processada é passada como parametro para o tesseract, que retorna uma string do texto abstraido da imagem.
                    result = googleCloudVisionManager.startReconizer(processedBitmap, this);
                    if (result == null) {
                        Toast.makeText(this, "Falha no reconhecimento!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Reconhecimento pelo Google Cloud Vision funcionou.");
                        //Armazena o resultado no intent para ser repassado para a ResultActivity.
                        intent.putExtra(RESULTED_TEXT, result);
                        //Carregamos a imagem em um array de bytes para conseguirmos envia-la por intent.
                        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        intent.putExtra(RESULTED_IMAGE, outputStream.toByteArray());
                        //Inicia a ResultActivity.
                        startActivity(intent);
                    }
                    break;
                default:
                    Toast.makeText(this, "Nenhuma API OCR selecionada!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Método para retornar a URI do bitmap.
     * @param src
     * @param format
     * @param quality
     * @return
     */
    public Uri getImageUri(Bitmap src, Bitmap.CompressFormat format, int quality) {
        //Stream que será usado para serializar a imagem.
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        //Comprimindo a imagem no formato JPG.
        src.compress(format, quality, os);
        //Retornando o caminho da imagem.
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), src, "title", null);
        return Uri.parse(path);
    }
}
