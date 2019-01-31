package com.claudiocaldeirao.platereader.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.claudiocaldeirao.platereader.R;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

/**
 * Atividade que exibe a localização do endereço obtido através do processo de OCR no GoogleMaps.
 *
 * Created by claudiocaldeirao on 17/01/2018.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    //Variáveis.
    private FusedLocationProviderClient mFusedLocationClient;
    private float radius = 5000;    //Em metros.
    private int maxAdresses = 50;
    //Constantes.
    private static final String GEOCODER_EXCEPTION = "GeocoderExeption";
    private String streetAdress;
    //Elementos da interface.
    private GoogleMap mMap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        //Recuperando a string do endereço da intent.
        Intent intent = getIntent();
        //Nome da rua a ser localizada.
        streetAdress = intent.getStringExtra(ResultActivity.TEXT_MESSAGE);
        //Pegando a ultima localização conhecida do dispositivo.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //Recuperando o raio de alcance da busca.
        //Multiplicamos por 1000 pq a precisão é em metros e o spiner está em Km.
        radius = (Float.valueOf(intent.getStringExtra(ResultActivity.RADIUS_SIZE))) * 1000;
        //Inicializa o mapa.
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Objeto mapa.
        mMap = googleMap;
        //Lista dos endereços encontrados apartir da string de busca.
        final List<Address> adressesList;
        //Verificando se existe permissão para utilizar o GPS.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
        }
        //Geocoder é responsável pela tarefa de retornar coordenadas dado uma string do endereço ou
        //retornar as informações do endereço dado uma cordenada.
        Geocoder geo = new Geocoder(this);
        try {
            //Tenta encontrar a lista de endereços possiveis para a string extraida da imagem.
            //Retorna no maximo 50 endereços possiveis.
            adressesList = geo.getFromLocationName(streetAdress, maxAdresses);
            //Tenta encontrar a rua dentro do raio determinado a partir da última localização conhecida do GPS.
            //O tamanho do raio é definido pelo usuário.
            //Recuperando a última localização conhecida do dispositivo.
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            //contador de marcadores usados.
                            int countMarks = 0;
                            //Caso a ultima localização do usuário exista.
                            if (location != null) {
                                Log.d(GEOCODER_EXCEPTION, "Existe uma ultima localização conhecida do GPS.");
                                //Se pelo menos um possível endereço foi encontrado.
                                if (adressesList != null) {
                                    Log.d(GEOCODER_EXCEPTION, "Encontramos pelo menos uma rua compativel com a string de busca.");
                                    //Percorre todos as localizações obtidas.
                                    for(int i = 0; i < adressesList.size(); i++) {
                                        //Coordenadas do endereço adressesList[i].
                                        double lat = adressesList.get(i).getLatitude();
                                        double lng = adressesList.get(i).getLongitude();
                                        //Criando um objeto do tipo Location para podermos aplicar o metodo distanceTo.
                                        Location possibleLocation = new Location("");
                                        possibleLocation.setLatitude(lat);
                                        possibleLocation.setLongitude(lng);
                                        LatLng latLng = new LatLng(lat, lng);
                                        //Distancia entre a possivel localização da rua e a última localização conhecida do dispositivo.
                                        float distance = location.distanceTo(possibleLocation);
                                        //Se a localização estiver dentro do raio de busca.
                                        if (distance < radius) {
                                            //Incrementa o contador de marcadores.
                                            countMarks++;
                                            //Adicionamos o marcador da mesma no mapa.
                                            mMap.addMarker(new MarkerOptions().position(latLng).title("Possible location [" + String.valueOf(i) + "]"));
                                            //Posiciona a view do mapa emcima do marcador do primeiro endereço retornado.
                                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                        } else {
                                            Log.d(GEOCODER_EXCEPTION, distance + " < " + radius);
                                            Log.d(GEOCODER_EXCEPTION, "O endereço não está dentro do raio de busca.");
                                        }
                                    }
                                    //Mostrando quantas localizações foram encontradas apartir da string e quantas estão dentro do raio de busca.
                                        Toast.makeText(MapsActivity.this, adressesList.size() + " localizações encontradas. \n"
                                                + countMarks + " estão dentro do raio de busca.", Toast.LENGTH_SHORT).show();

                                } else {
                                    Toast.makeText(MapsActivity.this, "Nenhuma localização encontrada.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.d(GEOCODER_EXCEPTION, "Última localização conhecida do dispositivo não encontrada.");
                                Toast.makeText(MapsActivity.this, "Última localização conhecida do dispositivo não encontrada.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            //Se o serviço estiver indisponivel ou o endereço for nulo.
            Log.d(GEOCODER_EXCEPTION, e.getMessage());
            Toast.makeText(this, "Erro ao retornar localização!", Toast.LENGTH_SHORT);
        }
    }
}
