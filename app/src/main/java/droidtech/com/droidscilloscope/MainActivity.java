package droidtech.com.droidscilloscope;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NumberPicker.OnValueChangeListener, RadioGroup.OnCheckedChangeListener,CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener{

    private static final String TAG = "USB-Example";

    // Variables GUI
    TextView textView;
    // TODO: Variables USB
    UsbManager mUsbManager;
    UsbDevice mUsbDevice;
    PendingIntent mPermissionIntent;
    UsbDeviceConnection mUsbDeviceConnection;
    UsbEndpoint epIN = null;
    UsbEndpoint epOUT = null;
    UsbTask usbTask;
    private XYPlot mySimpleXYPlot;
    SimpleXYSeries series1;
    SimpleXYSeries series2;
    LineAndPointFormatter series1Format;
    LineAndPointFormatter series2Format;
    ArrayList<Number> series3Numbers;
    ArrayList<Number> series4Numbers;
    ArrayList<Number> series5Numbers;
    ArrayList<Number> series6Numbers;
    public String modo="low_mode";
    public byte[] dataOut = {'0'};
    public boolean cambio= false;
    NumberPicker np=null;
    NumberPicker np2=null;
    boolean conexion_usb= false;
    RadioButton rbc1;
    RadioButton rbc2;
    RadioButton rbd;
    RadioGroup rg1;
    RadioGroup rg2;
    RadioGroup rg3;
    boolean series1Removida=false;
    boolean series2Removida=false;
    String freqX;
    String freqY;
    String Tx;
    String Ty;
    double vpp=5.0d;
    int offset=0;
    double offset1=130.5;
    double offset2=126.5;
    double densidadPuntos=0.1;
    int cantidadMuestras=151;
    boolean peticionLog1=false;
    boolean peticionLog2=false;
    int dataNumX=0;
    int dataNumY=0;
    CheckBox filterCheck1;
    CheckBox filterCheck2;
    boolean filterBoolean1=true;
    boolean filterBoolean2=true;
    SeekBar barraX1;
    SeekBar barraX2;
    // TODO: Al conectar a un dispositvo USB se solicita un permiso al usuario
    // este broadcast se encarga de recoger la respuesta del usuario.
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // TODO: Al aceptar el permiso del usuario.
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {

                    //UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "Permiso aceptado");
                        processComunicationUSB();
                    } else {
                        Log.e(TAG, "Permiso denegado");
                    }
                }
            }


            // TODO: Al desconectar el dispositivo USB cerramos las conexiones y liberamos la variables.
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {

                    usbTask.cancel(true);
                    mUsbDeviceConnection.close();
                    finish();

                    // call your method that cleans up and closes communication with the device
                }
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // esto es para ocultar el teclado
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        barraX1= (SeekBar) findViewById(R.id.barraX1);
        barraX2= (SeekBar) findViewById(R.id.barraX2);

        barraX2.setOnSeekBarChangeListener(this);
        barraX1.setOnSeekBarChangeListener(this);
        rg1 = (RadioGroup) findViewById(R.id.modGroup);
        rg1.setOnCheckedChangeListener(this);
        rg2= (RadioGroup) findViewById(R.id.rgSignalType);
        rg2.setOnCheckedChangeListener(this);
        rg3= (RadioGroup) findViewById(R.id.triggerGroup);
        rg3.setOnCheckedChangeListener(this);

        filterCheck1= (CheckBox) findViewById(R.id.filterCheck1);
        filterCheck1.setOnCheckedChangeListener(this);
        filterCheck2= (CheckBox) findViewById(R.id.filterCheck2);
        filterCheck2.setOnCheckedChangeListener(this);
        mySimpleXYPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

        //mySimpleXYPlot.setZoomEnabled(true);
        // Creamos dos arrays de prueba. En el caso real debemos reemplazar
        series3Numbers = new ArrayList<Number>();

        series4Numbers = new ArrayList<Number>();
        series5Numbers = new ArrayList<Number>();
        // estos datos por los que realmente queremos mostrar
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String seleccion = sharedPref.getString("densidadPuntos", "0");
        if(seleccion.equals("0")){
            densidadPuntos=0.1;
        }
         if(seleccion.equals("1")) {
            densidadPuntos=0.05;
        }

        series3Numbers = crearSerieX(0);
        series4Numbers= crearSerieY();
        series5Numbers= crearSerieY2();


        series1 = new SimpleXYSeries(series3Numbers,series4Numbers, "Ch1");
        series2 = new SimpleXYSeries(series3Numbers,series5Numbers, "Ch2");
        //----------------------------------------------------------------------------
        series1Format = new LineAndPointFormatter();
        series2Format = new LineAndPointFormatter();
        //-------------------------------------------------------------------------
        //series1Format.setPointLabelFormatter(new PointLabelFormatter());
        //-------------------------------------------------------------------------
        series1Format.configure(getApplicationContext(),
                R.xml.formater_yellow);
        series2Format.configure(getApplicationContext(),
                R.xml.formater_green);

        //-----------------------------------------------------------------------
        series1Format.setInterpolationParams(
                new CatmullRomInterpolator.Params(2, CatmullRomInterpolator.Type.Centripetal));
        series2Format.setInterpolationParams(
                new CatmullRomInterpolator.Params(2, CatmullRomInterpolator.Type.Centripetal));

        //-----------------------------------------------------------------------
        mySimpleXYPlot.addSeries(series1,series1Format);
        mySimpleXYPlot.addSeries(series2,series2Format);
        mySimpleXYPlot.setGridPadding(10,10,30,20);
        mySimpleXYPlot.setDomainBoundaries(0, BoundaryMode.FIXED,10,BoundaryMode.FIXED);
        mySimpleXYPlot.setRangeBoundaries(-10, BoundaryMode.FIXED,10,BoundaryMode.FIXED);
        mySimpleXYPlot.setRangeTopMax(20);
        mySimpleXYPlot.setDomainStep(XYStepMode.SUBDIVIDE,11);
        mySimpleXYPlot.setRangeStep(XYStepMode.SUBDIVIDE,11);
        mySimpleXYPlot.setRangeLabel("Voltaje[Volts]");
        mySimpleXYPlot.setDomainLabel("Tiempo");
        XYGraphWidget graphWidget= mySimpleXYPlot.getGraphWidget();
        Paint pincel =graphWidget.getGridBackgroundPaint();
        Paint pincel2= new Paint();
        pincel2.setColor(Color.BLACK);
        graphWidget.setGridBackgroundPaint(pincel2);
        mySimpleXYPlot.setGraphWidget(graphWidget);
        //mySimpleXYPlot.centerOnDomainOrigin(5.0);
        mySimpleXYPlot.setDomainRightMax(10.0);
        mySimpleXYPlot.setDomainUpperBoundary(10.0, BoundaryMode.FIXED);

         np= (NumberPicker) findViewById(R.id.np);
        String [] tiempos= {"10us/div","20us/div","50us/div","100us/div",
                "200us/div","400us/div","800us/div","1ms/div","2ms/div","4ms/div","5ms/div","10ms/div",
                "20ms/div","1s/div","1.5s/div"};
        np.setMaxValue(14);
        np.setMinValue(0);
        np.setDisplayedValues(tiempos);
        np.setValue(13);
        np.setWrapSelectorWheel(false);
        np.setOnValueChangedListener(MainActivity.this);

        np2= (NumberPicker) findViewById(R.id.volDiv);
        String [] rangos= {"1V/div","2V/div","3V/div","4V/div"};
        np2.setMaxValue(3);
        np2.setMinValue(0);
        np2.setDisplayedValues(rangos);
        np2.setValue(1);
        np2.setWrapSelectorWheel(false);
        np2.setOnValueChangedListener(MainActivity.this);
        //np.clearFocus();


    }



    @Override
    protected void onResume() {
        super.onResume();

        //TODO: Solicitamos permiso al usuario
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        //TODO: Registro del Broadcast
        registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String seleccion = sharedPref.getString("coloresGrafica1", "0");
        String seleccion2 = sharedPref.getString("coloresGrafica2", "0");
        boolean sombreado = sharedPref.getBoolean("sombreado",false);
        boolean boolOffset = sharedPref.getBoolean("offset",false);
        if(boolOffset){
            offset=128;
        }
        else {
            offset=0;
        }
        String seleccion3 = sharedPref.getString("vpp","0");
        switch (seleccion3){
            case "0":
                vpp=5.0d;
                break;
            case "1":
                vpp=10.0d;
                break;
            case "2":
                vpp=20.0d;
                break;
            case "3":
                vpp=30.0d;
                break;
        }
        switch (seleccion){
            case "0":
                series1Format.configure(getApplicationContext(), R.xml.formater_yellow);
                mySimpleXYPlot.invalidate();
                break;

            case "1":

                series1Format.configure(getApplicationContext(),R.xml.formater_black);
                mySimpleXYPlot.invalidate();
                break;
            case "2":

                series1Format.configure(getApplicationContext(),R.xml.formater_blue);
                mySimpleXYPlot.invalidate();
                break;
        }
        switch (seleccion2){
            case "0":
                series2Format.configure(getApplicationContext(), R.xml.formater_red);
                mySimpleXYPlot.invalidate();
                break;

            case "1":
                series2Format.configure(getApplicationContext(),R.xml.formater_green);
                mySimpleXYPlot.invalidate();
                break;
            case "2":

                series2Format.configure(getApplicationContext(),R.xml.formater_blue);
                mySimpleXYPlot.invalidate();
                break;
        }
        if(sombreado)
        {
            series1Format.enableShadows();
            series2Format.enableShadows();
        }
        else
        {
            series1Format.disableShadows();
            series2Format.disableShadows();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (this.mUsbReceiver != null) {
            unregisterReceiver(mUsbReceiver);
        }

    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        if(picker.getId()==R.id.np) {
            if (conexion_usb == true) {
                switch (newVal) {

                    case 0:
                        dataOut[0] = 'a';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[10us/DIV]");
                        mySimpleXYPlot.setDomainLabel("10us/DIV");
                        Log.d("envio ", "a");
                        break;
                    case 1:
                        dataOut[0] = 'b';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[20us/DIV]");
                        mySimpleXYPlot.setDomainLabel("20us/DIV");
                        Log.d("envio ", "b");
                        break;
                    case 2:
                        dataOut[0] = 'c';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[50us/DIV]");
                        mySimpleXYPlot.setDomainLabel("50us/DIV");
                        break;
                    case 3:
                        dataOut[0] = 'd';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[100us/DIV]");
                        mySimpleXYPlot.setDomainLabel("100us/DIV");
                        Toast.makeText(this, "Modo muestreo equivalente", Toast.LENGTH_SHORT).show();
                        break;
                    case 4:
                        dataOut[0] = 'e';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[200us/DIV]");
                        mySimpleXYPlot.setDomainLabel("200us/DIV");
                        break;
                    case 5:
                        dataOut[0] = 'f';
                        new Envio().run();
                        modo = "high_mode";
                        series1.setTitle("Tiempo[400us/DIV]");
                        mySimpleXYPlot.setDomainLabel("400us/DIV");
                        break;
                    case 6:
                        dataOut[0] = 'g';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[800us/DIV]");
                        mySimpleXYPlot.setDomainLabel("800us/DIV");
                        break;
                    case 7:
                        dataOut[0] = 'h';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[1ms/DIV]");
                        mySimpleXYPlot.setDomainLabel("1ms/DIV");
                        break;
                    case 8:
                        dataOut[0] = 'i';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[2ms/DIV]");
                        mySimpleXYPlot.setDomainLabel("2ms/DIV");
                        break;
                    case 9:
                        dataOut[0] = 'j';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[4ms/DIV]");
                        mySimpleXYPlot.setDomainLabel("4ms/DIV");
                        break;
                    case 10:
                        dataOut[0] = 'k';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[5ms/DIV]");
                        mySimpleXYPlot.setDomainLabel("5ms/DIV");
                        break;
                    case 11:
                        dataOut[0] = 'l';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[10ms/DIV]");
                        mySimpleXYPlot.setDomainLabel("10ms/DIV");
                        break;
                    case 12:
                        dataOut[0] = 'm';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[20ms/DIV]");
                        mySimpleXYPlot.setDomainLabel("20ms/DIV");
                        break;
                    case 13:
                        dataOut[0] = '0';
                        modo = "low_mode";
                        new Envio().run();
                        break;
                    case 14:
                        dataOut[0] = '1';
                        modo = "low_mode";
                        new Envio().run();
                        break;

                }
            } else {
                Toast.makeText(this, "No estas conectado, por favor reconecta", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            switch (newVal){
                case 0:
                    mySimpleXYPlot.setRangeBoundaries(-5, BoundaryMode.FIXED,5,BoundaryMode.FIXED);
                    mySimpleXYPlot.setRangeTopMax(20);
                    mySimpleXYPlot.setRangeStep(XYStepMode.SUBDIVIDE,11);
                    mySimpleXYPlot.invalidate();
                    break;
                case 1:
                    mySimpleXYPlot.setRangeBoundaries(-10, BoundaryMode.FIXED,10,BoundaryMode.FIXED);
                    mySimpleXYPlot.setRangeTopMax(20);
                    mySimpleXYPlot.setRangeStep(XYStepMode.SUBDIVIDE,11);
                    mySimpleXYPlot.invalidate();
                    break;
                case 2:
                    mySimpleXYPlot.setRangeBoundaries(-15, BoundaryMode.FIXED,15,BoundaryMode.FIXED);
                    mySimpleXYPlot.setRangeTopMax(20);
                    mySimpleXYPlot.setRangeStep(XYStepMode.SUBDIVIDE,11);
                    mySimpleXYPlot.invalidate();
                    break;
                case 3:
                    mySimpleXYPlot.setRangeBoundaries(-20, BoundaryMode.FIXED,20,BoundaryMode.FIXED);
                    mySimpleXYPlot.setRangeTopMax(20);
                    mySimpleXYPlot.setRangeStep(XYStepMode.SUBDIVIDE,11);
                    mySimpleXYPlot.invalidate();
                    break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
       if(buttonView.getId()==R.id.filterCheck1){

           if(isChecked){
               filterBoolean1=true;
           }
           else {
               filterBoolean1=false;
           }
       }
        if(buttonView.getId()==R.id.filterCheck2){
            if(isChecked){
                filterBoolean2=true;
            }
            else {
                filterBoolean2=false;
            }
        }


    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        double offset = (-progress / 100.0) * 5.0;
        if(seekBar.getId()==R.id.barraX1) {

            series1 = desplazarX(series1, offset);
        }
        else{
            series2 = desplazarX(series2, offset);
        }
        mySimpleXYPlot.invalidate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View v) {

    }


    private class Envio extends Thread {
        @Override
        public void run() {
            mUsbDeviceConnection.bulkTransfer(epOUT, dataOut, 1, 1000);
        }

    }

    private class UsbTask extends AsyncTask<String, Object, String>
    {

        @Override
        protected String doInBackground(String... params)
        {
            int i = 0;
            byte[] datos;
            double[] datosDouble= new double[cantidadMuestras+4];
            double[] prom= new double[cantidadMuestras+4];
            float y=5;
            ByteBuffer mBuffer = ByteBuffer.allocate(cantidadMuestras+4);
            ByteBuffer mBuffer2 = ByteBuffer.allocate(cantidadMuestras+4);
            datos = mBuffer2.array();

            UsbRequest inRequest = new UsbRequest();
            inRequest.initialize(mUsbDeviceConnection, epIN);
            dataOut[0]='0';
            modo="low_mode";
            new Envio().run();
                while (!usbTask.isCancelled()) {
                    if (modo == "low_mode")
                    {

                        while (inRequest.queue(mBuffer, 1) == true)
                        {
                            mUsbDeviceConnection.requestWait();
                            i++;

                            if (i >0)
                            {
                                i = 0;
                                // Recogemos los datos que recibimos en un

                                datos = mBuffer.array();
                                if (datos[0] < 0) {
                                    y= (int)datos[0]+256;
                                    y= (y*5.0f)/255.0f;

                                } else {
                                    y= (int)datos[0];
                                    y= (y*5.0f)/255.0f;
                                }
//
                                publishProgress(y);
                                break;

                            }
                        }
                    }
                    if(modo=="high_mode") {
                        mUsbDeviceConnection.bulkTransfer(epIN, datos, cantidadMuestras+4, 1000);

                            if ((char) datos[cantidadMuestras+1] == 'x') {
                                if (peticionLog1){
                                    dataLog(datos,cantidadMuestras,1);
                                    peticionLog1=false;
                                }

                                char multiplicador = ' ';
                                char multiplicador2 = 'u';
                                double freqNumX = 1000000 / ((byteToInt(datos[cantidadMuestras+2]) + 256 * byteToInt(datos[cantidadMuestras+3])) * 0.083333 * 8);
                                double TNumX = (byteToInt(datos[cantidadMuestras+2]) + 256 * byteToInt(datos[cantidadMuestras+3])) * 0.083333 * 8;

                                //freq= Byte.toString(byteFreq2)+ Byte.toString(byteFreq1);
                                if (freqNumX > 1000) {
                                    freqNumX = freqNumX / 1000;
                                    multiplicador = 'k';
                                }

                                if (TNumX > 1000) {
                                    TNumX = TNumX / 1000;
                                    multiplicador2 = 'm';
                                }
                                freqX = String.format("%5.2f%c", freqNumX, multiplicador);
                                Tx = String.format("%5.2f%cs", TNumX, multiplicador2);


                                freqNumX = 1000000 / ((byteToInt(datos[cantidadMuestras+2]) + 256*byteToInt(datos[cantidadMuestras+3])) * 0.083333 * 8);
                                if (freqNumX < 25) {
                                    freqX = String.format("F<25");
                                    Tx = String.format("T>40ms");
                                }

                                datosDouble= byteToDouble(datos);
                                prom[0]=datosDouble[0];
                                prom[1]=datosDouble[1];
                                prom[cantidadMuestras-1]=datosDouble[cantidadMuestras-1];
                                if(filterBoolean1) {
                                    for (int k = 2; k < cantidadMuestras - 1; k++) {
                                        prom[k] = (datosDouble[k - 1] + datosDouble[k] + datosDouble[k + 1]) / 3;
                                    }
                                    for (int k = 0; k < cantidadMuestras; k++) {
                                        series4Numbers.set(k, (Number) (((prom[k] - offset1) * (vpp / 255.0d))));

                                    }
                                }
                                else {
                                    for (int k = 0; k < cantidadMuestras; k++) {
                                        series4Numbers.set(k, (Number) (((datosDouble[k] - offset1) * (vpp / 255.0d))));

                                    }
                                }

                            }
                            if ((char) datos[cantidadMuestras+1] == '2')
                            {
                                if(peticionLog2){
                                    dataLog(datos,cantidadMuestras,2);
                                    peticionLog2=false;
                                }
                                char multiplicador=' ';
                                char multiplicador2='u';
                                double freqNumy= 1000000/((byteToInt(datos[cantidadMuestras+2])+256*byteToInt(datos[cantidadMuestras+3]))*0.083333333*8);
                                double TNumy= (byteToInt(datos[cantidadMuestras+2])+256*byteToInt(datos[cantidadMuestras+3]))*0.083333*8;
                                //freq= Byte.toString(byteFreq2)+ Byte.toString(byteFreq1);
                                if(freqNumy>1000){
                                    freqNumy=freqNumy/1000;
                                    multiplicador='k';
                                }
                                if (TNumy>1000){
                                    TNumy=TNumy/1000;
                                    multiplicador2='m';
                                }
                                freqY= String.format("%5.2f%c",freqNumy,multiplicador);
                                Ty= String.format("%5.2f%cs",TNumy,multiplicador2);
                                freqNumy= 1000000/((byteToInt(datos[cantidadMuestras+2])+256*byteToInt(datos[cantidadMuestras+3]))*0.083333333*8);

                                if (freqNumy<25)
                                {
                                    freqY= String.format("F<25");
                                    Ty= String.format("T>40ms");
                                }


                                datosDouble= byteToDouble(datos);
                                prom[0]=datosDouble[0];
                                prom[1]=datosDouble[1];
                                prom[cantidadMuestras-1]=datosDouble[cantidadMuestras-1];
                                if(filterBoolean2) {
                                    for (int k = 2; k < cantidadMuestras - 1; k++) {
                                        prom[k] = (datosDouble[k - 1] + datosDouble[k] + datosDouble[k + 1]) / 3;
                                    }
                                    for (int k = 0; k < cantidadMuestras; k++) {
                                        series5Numbers.set(k, (Number) (((prom[k] - offset2) * (vpp / 255.0d))));

                                    }
                                }
                                else {
                                    for (int k = 0; k < cantidadMuestras; k++) {
                                        series5Numbers.set(k, (Number) (((datosDouble[k] - offset2) * (vpp / 255.0d))));

                                    }
                                }
                            }




                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        publishProgress(y);
                    }

                }

            return null;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
           // super.onProgressUpdate(values);
            if(modo=="high_mode")
            {

                        series1 = reconstruir(series1, series4Numbers);
                        series2 = reconstruir(series2, series5Numbers);

                        mySimpleXYPlot.setTitle("F1: "+freqX+"Hz"+"  T1: "+Tx+"          F2: "+ freqY+"Hz"+"  T2: "+Ty);

                        mySimpleXYPlot.invalidate();


            }
            if(modo=="low_mode")
            {
                float entrada = (float) values[0];

                series1 = desplazar(series1, entrada);
                mySimpleXYPlot.invalidate();
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id)
        {
            case R.id.settings:
                Intent i = new Intent(MainActivity.this, Preferencias.class);
                startActivity(i);
                break;
            case R.id.saveCh1:
                peticionLog1=true;


                Toast.makeText(this,"Datos Guardados en Android/data/droidTech.com.droidscilloscope",Toast.LENGTH_SHORT).show();
                if(dataNumX>9){
                    dataNumX=0;
                }
                break;
            case R.id.saveCh2:
                peticionLog2=true;
                Toast.makeText(this,"Datos Guardados en Android/data/droidTech.com.droidscilloscope",Toast.LENGTH_SHORT).show();

                if(dataNumY>9){
                    dataNumY=0;
                }
                break;
            case R.id.connect:
                //TODO: Obtemos el Manager USB del sistema Android
                mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

                // TODO: Recuperamos todos los dispositvos USB detectados
                HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

                //TODO: en nuestor ejemplo solo conectamos un disposito asi que sera
                // el unico que encontraremos.
                Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                if (deviceIterator.hasNext()) {
                    mUsbDevice = deviceIterator.next();
                    Log.d(TAG, "Name: " + mUsbDevice.getDeviceName());
                    Log.d(TAG, "Protocol: " + mUsbDevice.getDeviceProtocol());
                    //TODO: Solicitamos el permiso al usuario.
                    mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
                    conexion_usb=true;
                } else {
                    Log.e(TAG, "Dispositvo USB no detectado.");
                }

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void processComunicationUSB() {

        boolean forceClaim = true;

        mUsbDeviceConnection = mUsbManager.openDevice(mUsbDevice);
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, "No se ha podido conectar con el dispositivo USB.");
            finish();
        }

        // TODO: getInterfase(1) Obtiene el tipo de comunicacion CDC (USB_CLASS_CDC_DATA)
        UsbInterface mUsbInterface = mUsbDevice.getInterface(1);

        // TODO: Obtenemos los Endpoints de entrada y salida para el interface que hemos elegido.
        for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {
            if (mUsbInterface.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {

                if (mUsbInterface.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN)
                    epIN = mUsbInterface.getEndpoint(i);
                else
                    epOUT = mUsbInterface.getEndpoint(i);
            }
        }

        mUsbDeviceConnection.claimInterface(mUsbInterface, forceClaim);

        // TODO: Mensaje de configuración para el Device.
        int baudRate = 115200;
        byte stopBitsByte = 1;
        byte parityBitesByte = 0;
        byte dataBits = 8;
        byte[] msg = {
                (byte) (baudRate & 0xff),
                (byte) ((baudRate >> 8) & 0xff),
                (byte) ((baudRate >> 16) & 0xff),
                (byte) ((baudRate >> 24) & 0xff),
                stopBitsByte,
                parityBitesByte,
                (byte) dataBits
        };

        mUsbDeviceConnection.controlTransfer(UsbConstants.USB_TYPE_CLASS | 0x01, 0x20, 0, 0, msg, msg.length, 5000);
        // (UsbConstants.USB_TYPE_CLASS | 0x01) 0x21 -> Indica que se envia un parametro/mensaje del Host al Device (movil a la placa leonardo)
        // 0x20 -> paramtro/mensaje SetLineCoding

        mUsbDeviceConnection.controlTransfer(UsbConstants.USB_TYPE_CLASS | 0x01, 0x22, 0x1, 0, null, 0, 0);
        // (UsbConstants.USB_TYPE_CLASS | 0x01) 0x21 -> Indica que se envia un parametro/mensaje del Host al Device (movil a la placa leonardo)
        // 0x22 -> paramtro/mensaje SET_CONTROL_LINE_STATE (DTR)
        // 0x1  -> Activado.
        // Mas info: http://www.usb.org/developers/devclass_docs/usbcdc11.pdf

        // TODO: Ejecutar en un hilo
        usbTask= new UsbTask();
        usbTask.execute();

        //TODO: Otro metod de obtener datos Syncronhus
        //while (true) {still valid, and can thus call cancel() to remove it.
        //Arrays.fill(buffer, (byte) 0);
        //int ret = mUsbDeviceConnection.bulkTransfer(epIN, buffer, buffer.length, TIMEOUT);
        //Log.d("USB","Return BulkTransfer: " + ret);
        //Log.d("USB","Buffer BulkTransfer: " + new String(buffer));
        //}

    }
    public ArrayList<Number> crearSerieY()
    {

        ArrayList<Number> array= new ArrayList<Number>();

        for(int j=0;j<cantidadMuestras;j++){
            array.add(Math.sin(0.6*3.14*(0.1*j))*2.5+2.5);
        }

        return array;
    }
    public ArrayList<Number> crearSerieY2()
    {

        ArrayList<Number> array= new ArrayList<Number>();

        for(int j=0;j<cantidadMuestras;j++){
            array.add(Math.cos(0.6*3.14*(0.1*j))*2.5+2.5);
        }

        return array;
    }
    public SimpleXYSeries reconstruir(SimpleXYSeries serie, ArrayList<Number> arrayList){
        int k =arrayList.size();

        for (int j=0;j<k;j++){
            serie.setY(arrayList.get(j),j);
        }

        return serie;
    }
    public ArrayList<Number> crearSerieX(int offset)
    {
        ArrayList<Number> array= new ArrayList<Number>();

        for(int g=0;g<cantidadMuestras;g++){
            array.add(g*densidadPuntos+offset);
        }
        return array;
    }
    public SimpleXYSeries desplazarX(SimpleXYSeries serie,double y) {

        for (int i=0; i<cantidadMuestras;i++){
            serie.setX(y+densidadPuntos*i, i);
        }

        return serie;
    }
    public SimpleXYSeries desplazar(SimpleXYSeries serie,double y) {
        for (int i=0; i<cantidadMuestras-1;i++){
            serie.setY(serie.getY(i+1),i);
        }
        serie.setY(y,cantidadMuestras-1);
        return serie;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

        if(group.getId()==R.id.modGroup) {


            switch (checkedId) {

                case R.id.rbc1:
                    if (series1Removida == true) {
                        mySimpleXYPlot.addSeries(series1, series1Format);
                        series1Removida = false;
                    }
                    mySimpleXYPlot.removeSeries(series2);
                    series2Removida = true;
                    mySimpleXYPlot.invalidate();

                    break;
                case R.id.rbc2:

                    mySimpleXYPlot.removeSeries(series1);
                    if (series2Removida == true) {
                        mySimpleXYPlot.addSeries(series2, series2Format);
                        series2Removida = false;
                    }
                    mySimpleXYPlot.invalidate();
                    series1Removida = true;
                    break;
                case R.id.rbd:
                    dataOut[0] = 'M';
                    modo = "high_mode";
                    new Envio().run();
                    if (series2Removida == true) {
                        mySimpleXYPlot.addSeries(series2, series2Format);
                        series2Removida = false;
                    }
                    if (series1Removida == true) {
                        mySimpleXYPlot.addSeries(series1, series1Format);
                        series1Removida = false;
                    }
                    mySimpleXYPlot.invalidate();

                    break;
            }
        }
        else if (group.getId()==R.id.rgSignalType)
        {
            switch (checkedId){
                case R.id.rbAC:

                    mySimpleXYPlot.setRangeBoundaries(-5, BoundaryMode.FIXED,5,BoundaryMode.FIXED);
                    mySimpleXYPlot.setRangeTopMax(20);
                    mySimpleXYPlot.setRangeStep(XYStepMode.SUBDIVIDE,11);
                    mySimpleXYPlot.invalidate();
                    break;
                case R.id.rbDC:
                    mySimpleXYPlot.setRangeBoundaries(-5, BoundaryMode.FIXED,0,BoundaryMode.FIXED);
                    mySimpleXYPlot.setRangeTopMax(20);
                    mySimpleXYPlot.setRangeStep(XYStepMode.SUBDIVIDE,11);
                    mySimpleXYPlot.invalidate();

                    break;
            }

        }
        else if(group.getId()==R.id.triggerGroup){
            switch (checkedId) {
                case R.id.rBL2H:
                    dataOut[0] = 'H';
                    new Envio().run();
                    break;
                case R.id.rbH2L:
                    dataOut[0] = 'L';
                    new Envio().run();
                    break;
            }
        }
    }

   public int byteToInt (byte num){
       int dato=0;
       if(num>0){
           dato=num;
       }
       else if((int)num<0){
           dato= num+256;
       }
       return dato;
   }
    public double[] byteToDouble (byte[] datos){
        double[] datosDouble= new double[datos.length];
        for(int i=0;i<datos.length;i++){
            if(datos[i]<0){
                datosDouble[i]=datos[i]+256;
            }
            else {
                datosDouble[i]=datos[i];
            }
        }
        return datosDouble;
    }
     public void dataLog(byte[] datosBytes,int cantidadDatos,int canalLog){
         String dataString="";
         String nombreArchivo="datosX.txt";
         if(canalLog==1){
             dataNumX++;
             nombreArchivo="datosX"+Integer.toString(dataNumX)+".txt";

         }
         else if(canalLog==2){
             dataNumY++;
             nombreArchivo="datosY"+Integer.toString(dataNumY)+".txt";
         }
         int[] datosInt= new int[cantidadDatos];
         for(int k=0;k<cantidadDatos;k++){
             if(datosBytes[k]<0){
                 datosInt[k]=datosBytes[k]+256;
             }
             else {
                 datosInt[k]=datosBytes[k];
             }
         }
         for(int i=0;i<cantidadDatos;i++){
             dataString= dataString+ Integer.toString(datosInt[i])+" ";
         }
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        // COMPROBACION DEL ALMACENAMIENTO EXTERNO
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Podremos leer y escribir en ella
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // En este caso solo podremos leer los datos
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // No podremos leer ni escribir en ella
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        if (mExternalStorageWriteable == true) {

            // O creamos un archivo usando un directorio personalizado
            File file2 = new File(getExternalFilesDir("DroidScope_logs"), nombreArchivo);

            // Escribimos algo en el archivo creado
            try {
                OutputStreamWriter OSW = new OutputStreamWriter(
                        new FileOutputStream(file2));
                OSW.write(dataString);
                OSW.flush();
                OSW.close();
            } catch (FileNotFoundException e) {
                Log.e("ERROR", "No ha sido posible crear el archivo" + e.toString());
            } catch (IOException e) {
                Log.e("ERROR", "No ha sido posible escribir en el archivo"
                        + e.toString());
            }

        } else {
            Log.d("ERROR", "No ha sido posible crear archivos/carpetas");
        }
    }

}
