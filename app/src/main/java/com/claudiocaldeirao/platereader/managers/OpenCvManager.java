package com.claudiocaldeirao.platereader.managers;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador dos métodos do OpenCV.
 *
 * Created by claudiocaldeirao on 31/10/2017.
 */
public class OpenCvManager extends Application {
    private static final String TAG = "Opencv_Time_Elapsed";

    /**
     * Método que agrupa todos os filtros de pré processamento de imagens aplicados no projeto.
     * @param  bitmap
     * @return
     */
    public Bitmap processImage(Bitmap bitmap) {
        //Medindo o tempo de execução do processo em millisegundos.
        long start = System.currentTimeMillis();
        Mat rgbMat = new Mat(); //Matriz da imagem original em RGB.
        Mat grayMat = new Mat();    //Matriz da imagem em tons de cinza.
        Mat srcMat = new Mat(); //Matriz de origem (binária).
        Mat dstMat = new Mat(); //Matriz de destino (binária).
        //Convertendo o bitmap em matriz para podermos aplicar os filtros.
        Utils.bitmapToMat(bitmap, rgbMat);
        //Convertendo a matriz colorida p/ tons de cinza.
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        //Resize.
        Imgproc.resize(grayMat, dstMat, new Size(1280, 768), 0, 0, Imgproc.INTER_CUBIC);
        //Pegando o valor máximo/minimo da matriz.
        MinMaxLocResult minMax = Core.minMaxLoc(dstMat);
        //Limiar para a binarização.
        double limiar = minMax.maxVal - (minMax.maxVal/4);
        //Aplicando o filtro gaussiano para remover ruidos na imagem.
        Imgproc.GaussianBlur(dstMat, srcMat,new Size(7, 7),3,0);

        //DEPRECATED
        //Aplicamos binarização simples (agora temos um fundo preto e o conteúdo em branco), para aplicarmos a correção geométrica.
        //Imgproc.threshold(srcMat, dstMat, limiar, minMax.maxVal, Imgproc.THRESH_BINARY);
        //Aplicamos a correção geométrica.
        //Método de correção apenas no plano (Deprecated).
        //srcMat = AffineCorrection(dstMat);
        //END DEPRECATED.

        //TENTANDO REMOVER BORDA.
        //srcMat = removeBorders(srcMat);

        //Aplicamos a binarização (invertida, para tornarmos o fundo branco e o conteúdo em preto).
        Imgproc.threshold(srcMat, dstMat, limiar, minMax.maxVal, Imgproc.THRESH_BINARY_INV);
        //Imgproc.adaptiveThreshold(srcMat, dstMat, minMax.maxVal, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 1);
        //Convertendo de volta pra bitmap.
        Bitmap newBitmap = Bitmap.createBitmap(dstMat.width(), dstMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dstMat, newBitmap);
        //Computa o tempo de execução do processo.
        long elapsed = System.currentTimeMillis() - start;
        Log.d(TAG, String.valueOf(elapsed) + " milisegundos. (pp)");
        //Retorna a imagem processada.
        return  newBitmap;
    }

    /**
     * Método de correção geométrica da placa.
     * @param src
     * @deprecated
     * @return
     *
     * 1º Constrói um triangulo de menor area a partir de um vetor de pixels > 0.
     * 2º Computa o angulo de inclinação do retangulo.
     * 3º Aplica a correção na matriz original.    *
     */
    private Mat AffineCorrection(Mat src) {
        //Matriz que armazenará o resultado do método warpaffine.
        Mat dst = new Mat();
        //Matriz que irá armazenar as coordenadas dos pixels com valor > 0.
        Mat points = Mat.zeros(src.size(),src.type());
        //Método que retorna as coordenadas dos pixels > 0.
        Core.findNonZero(src, points);
        //Matrizes auxiliares na conversão da matriz de coordenadas dos pontos > 0 de Mat para MatOfPoint2f.
        MatOfPoint points1f = new MatOfPoint(points);
        MatOfPoint2f points2f = new MatOfPoint2f(points1f.toArray());
        //Geramos um retangulo de menor área possível apartir das coordenadas dos pontos.
        RotatedRect minAreaRect = Imgproc.minAreaRect(points2f);
        //Angulo de inclinação do retangulo (valor no intervalo [-90, 0]).
        double angle = minAreaRect.angle;
        //Correção da inclinação.
        if(angle < -45) {
            //Se o angulo de inclinação for menor que -45, então precisaremos rotacionar o angulo + 90º.
            angle = angle + 90;
        } else {
            //Se não rotacionaremos o inverso do angulo.
            angle = -angle;
        }
        //Ponto central da matriz.
        Point center = new Point(src.width()/2, src.height()/2);
        //Geramos a matriz de rotação apartir do ponto central e do angulo.
        Mat rotatedMat = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        //Aplicamos o método de correção geométrica na matriz original e armazenamos o resultado em dst.
        Imgproc.warpAffine(src, dst, rotatedMat, src.size(), Imgproc.INTER_CUBIC);
        return dst;
    }

    /**
     * Método que efetua a correção geométrica da imagem apartir dos pontos obtidos pela boundingbox.
     * @param bitmap
     * @param coords
     * @return
     */
    public Bitmap PerspectiveCorrection(Bitmap bitmap, ArrayList<Point> coords) {
        //Medindo o tempo de execução do processo em millisegundos.
        long start = System.currentTimeMillis();
        //Matriz de origem da imagem.
        Mat imgSrc = new Mat();
        //Converte a bitmap para mat.
        Utils.bitmapToMat(bitmap, imgSrc);
        //Matriz de destino da imagem.
        Mat imgDst = new Mat(768,1024,CvType.CV_32FC2);
        //Mat src = new Mat(4,1,CvType.CV_32FC2);
        MatOfPoint m1 = new MatOfPoint();
        m1.fromList(coords);
        MatOfPoint2f m2 = new MatOfPoint2f();
        m1.convertTo(m2, CvType.CV_32F);
        //Criando uma matriz a partir das coordenadas obtidas pela boundingbox.
        //Coordenadas de destino correspondentes a coords p/ calcular a matriz de transformação.
        ArrayList<Point> dst = new ArrayList<>();
        dst.add(new Point(0,0));
        dst.add(new Point(1024,0));
        dst.add(new Point(1024,768));
        dst.add(new Point(0,768));
        MatOfPoint n1 = new MatOfPoint();
        n1.fromList(dst);
        MatOfPoint2f n2 = new MatOfPoint2f();
        n1.convertTo(n2, CvType.CV_32F);
        //Calculando a matriz de transformação 3x3.
        Mat transformationMat = Imgproc.getPerspectiveTransform(m2, n2);
        //Efetua a correção de perspectiva.
        Imgproc.warpPerspective(imgSrc, imgDst, transformationMat, imgDst.size());
        //Converte a nova matriz para bitmap.
        Bitmap bmp = Bitmap.createBitmap(imgDst.width(), imgDst.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgDst, bmp);
        //Computa o tempo de execução do processo.
        long elapsed = System.currentTimeMillis() - start;
        Log.d(TAG, String.valueOf(elapsed) + " milisegundos. (cg)");
        //Retorna a nova bitmap.
        return bmp;
    }

    /**
     * Método que encontra as bordas brancas da placa e as remove.
     * @param src
     * @deprecated
     * @return
     */
    private Mat removeBorders(Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        //Mat dst = Mat.zeros(src.size(), CvType.CV_8UC3);
        Mat dst = src.clone();
        Mat hierarchy = new Mat();
        //Retorna todos os contornos da imagem categorizados em niveis de hierárquia.
        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //TENTATIVA DE APAGAR OS CONTORNOS BRANCOS DA PLACA.
        //NÃO FUNCIONOU DEVIDO A PROBLEMAS NÃO PREVISTOS NA HIERÁRQUIA.
        //(QUALQUER FALHA NO CONTORNO DA PLACA RESULTAVA EM UMA HIERARQUIA DO MESMO LEVEL DAS LETRAS NO INTERIOR DA PLACA).
        //Imgproc.drawContours(dst, allContours, -1, new Scalar(255, 255, 255), -1, 8, fullyHierarchy, 3, new Point());
        //Imgproc.drawContours(dst, externalContours, -1, new Scalar(0, 0, 0), -1, 8, partialHierarchy, 0, new Point());

        //Desenha os contornos.
        Imgproc.drawContours(dst, contours, -1, new Scalar(255, 255, 255), -1, 8, hierarchy, 1, new Point());

        return dst;
    }

    /**
     * Método para setar o contraste e o brilho da imagem.
     * @param bitmap
     * @param contrastParam
     * @param brightnessParam
     */
    public Bitmap setContrastAndBrightness(Bitmap bitmap, int contrastParam, int brightnessParam) {
        //Compensação em relação ao ponto central.
        double dContrast = contrastParam / 50.0;
        int iBrightness = brightnessParam - 50;
        //Matriz de origem da imagem,
        Mat src = new Mat();
        //Matriz de destino da imagem.
        Mat dst = new Mat();
        //Conversão da imagem para matriz.
        Utils.bitmapToMat(bitmap, src);
        //Convertendo a matriz com base nos parametros.
        src.convertTo(dst,-1, dContrast, iBrightness);
        //Convertendo de volta.
        Bitmap bmp = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, bmp);
        //Retornando a imagem.
        return bmp;
    }
}