package droidtech.com.droidscilloscope;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NumberPicker.OnValueChangeListener, RadioGroup.OnCheckedChangeListener{

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
    boolean conexion_usb= false;
    RadioButton rbc1;
    RadioButton rbc2;
    RadioButton rbd;
    RadioGroup rg1;
    boolean series1Removida=false;
    boolean series2Removida=false;
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
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        //textView = (TextView) findViewById(R.id.textView1);
        rbc1= (RadioButton) findViewById(R.id.rbc1);
        rbc2= (RadioButton) findViewById(R.id.rbc2);
        rbd= (RadioButton) findViewById(R.id.rbd);
        rg1 = (RadioGroup) findViewById(R.id.modGroup);
        rg1.setOnCheckedChangeListener(this);

        mySimpleXYPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

        // Creamos dos arrays de prueba. En el caso real debemos reemplazar
        series3Numbers = new ArrayList<Number>();

        series4Numbers = new ArrayList<Number>();
        series5Numbers = new ArrayList<Number>();
        // estos datos por los que realmente queremos mostrar
        series3Numbers = crearSerieX();
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
                new CatmullRomInterpolator.Params(3, CatmullRomInterpolator.Type.Centripetal));
        series2Format.setInterpolationParams(
                new CatmullRomInterpolator.Params(3, CatmullRomInterpolator.Type.Centripetal));

        //-----------------------------------------------------------------------
        mySimpleXYPlot.addSeries(series1,series1Format);
        mySimpleXYPlot.addSeries(series2,series2Format);
        mySimpleXYPlot.setGridPadding(10,10,30,20);
        mySimpleXYPlot.setDomainBoundaries(0, BoundaryMode.FIXED,10,BoundaryMode.FIXED);
        mySimpleXYPlot.setRangeBoundaries(0, BoundaryMode.FIXED,6,BoundaryMode.FIXED);
        mySimpleXYPlot.setRangeTopMax(20);
        mySimpleXYPlot.setDomainStep(XYStepMode.SUBDIVIDE,11);
        mySimpleXYPlot.setRangeStep(XYStepMode.SUBDIVIDE,7);
        mySimpleXYPlot.setRangeLabel("Voltaje");
        mySimpleXYPlot.setDomainLabel("Tiempo");


         np= (NumberPicker) findViewById(R.id.np);
        String [] tiempos= {"10us/div","20us/div","50us/div","100us/div",
                "200us/div","400us/div","800us/div","1ms/div","2ms/div","4ms/div","5ms/div","10ms/div",
                "20ms/div","1s/div","1.5s/div"};
        np.setMaxValue(14);
        np.setMinValue(0);
        np.setDisplayedValues(tiempos);
        np.setValue(1);
        np.setWrapSelectorWheel(false);
        np.setOnValueChangedListener(MainActivity.this);
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
        Paint pincel = new Paint();
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
    public void onClick(View v) {
        if(conexion_usb==true) {
            switch (v.getId()) {
                case R.id.mySimpleXYPlot:
                    Toast.makeText(this, "pulsaste", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btn1us:
                        dataOut[0] = 'a';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[10us/DIV]");
                        mySimpleXYPlot.setDomainLabel("10us/DIV");
                        Log.d("envio ", "a");

                    break;
                case R.id.btn2us:
                        dataOut[0] = 'b';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[20us/DIV]");
                        mySimpleXYPlot.setDomainLabel("20us/DIV");
                        Log.d("envio ", "b");
                    break;
                case R.id.btn5us:
                        dataOut[0] = 'c';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[50us/DIV]");
                        mySimpleXYPlot.setDomainLabel("50us/DIV");
                    break;
                case R.id.btn10us:

                        dataOut[0] = 'd';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[100us/DIV]");
                        mySimpleXYPlot.setDomainLabel("100us/DIV");
                    break;
                case R.id.btn20us:
                        dataOut[0] = 'e';
                        modo = "high_mode";
                        new Envio().run();
                        series1.setTitle("Tiempo[200us/DIV]");
                        mySimpleXYPlot.setDomainLabel("200us/DIV");
                    break;
                case R.id.btn40us:
                    dataOut[0] = 'f';
                    new Envio().run();
                    modo = "high_mode";
                    series1.setTitle("Tiempo[400us/DIV]");
                    mySimpleXYPlot.setDomainLabel("400us/DIV");
                    break;
                case R.id.btn80us:
                    dataOut[0] = 'g';
                    modo = "high_mode";
                    new Envio().run();
                    series1.setTitle("Tiempo[800us/DIV]");
                    mySimpleXYPlot.setDomainLabel("800us/DIV");
                    break;
                case R.id.btn100us:
                    dataOut[0] = 'h';
                    modo = "high_mode";
                    new Envio().run();
                    series1.setTitle("Tiempo[1ms/DIV]");
                    mySimpleXYPlot.setDomainLabel("1ms/DIV");
                    break;
                case R.id.btn200us:
                    dataOut[0] = 'i';
                    modo = "high_mode";
                    new Envio().run();
                    series1.setTitle("Tiempo[2ms/DIV]");
                    mySimpleXYPlot.setDomainLabel("2ms/DIV");
                    break;
                case R.id.btn400us:
                    dataOut[0] = 'j';
                    modo = "high_mode";
                    new Envio().run();
                    series1.setTitle("Tiempo[4ms/DIV]");
                    mySimpleXYPlot.setDomainLabel("4ms/DIV");
                    break;
                case R.id.btn500us:
                    dataOut[0] = 'k';
                    modo = "high_mode";
                    new Envio().run();
                    series1.setTitle("Tiempo[5ms/DIV]");
                    mySimpleXYPlot.setDomainLabel("5ms/DIV");
                    break;
                case R.id.btn1000us:
                    dataOut[0] = 'l';
                    modo = "high_mode";
                    new Envio().run();
                    series1.setTitle("Tiempo[10ms/DIV]");
                    mySimpleXYPlot.setDomainLabel("10ms/DIV");
                    break;
                case R.id.btn2000us:
                    dataOut[0] = 'm';
                    modo = "high_mode";
                    new Envio().run();
                    series1.setTitle("Tiempo[20ms/DIV]");
                    mySimpleXYPlot.setDomainLabel("20ms/DIV");
                    break;
                case R.id.btn25ms:
                    dataOut[0] = '0';
                    modo = "low_mode";
                    new Envio().run();
                    break;
                case R.id.btn40ms:
                    dataOut[0] = '1';
                    modo = "low_mode";
                    new Envio().run();
                    break;

            }
        }
        else
        {
            Toast.makeText(this, "No estas conectado, por favor reconecta", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        if(conexion_usb==true) {
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
        }
        else
        {
            Toast.makeText(this,"No estas conectado, por favor reconecta",Toast.LENGTH_SHORT).show();
        }
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
            byte[] datosNull;
            ArrayList<Number> datosDouble= crearSerieY();

            String line = new String();

            Number [] muestreo={0.0};
            float y=5;
            int bufferMaxLength = epIN.getMaxPacketSize();
            ByteBuffer mBuffer = ByteBuffer.allocate(103);
            ByteBuffer mBuffer2 = ByteBuffer.allocate(103);
            datos = mBuffer2.array();
            datosNull= mBuffer2.array();
            for(int s=0; s<101;s++){
                datosNull[s]=100;
            }

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
                                publishProgress(y,muestreo);
                                break;

                            }
                        }
                    }
                    if(modo=="high_mode") {
                        mUsbDeviceConnection.bulkTransfer(epIN, datos, 103, 1000);
                        if ((char)datos[101]=='T')
                        {
                            if((char)datos[102]=='x') {
                                Log.d("No Trigger","Sin trigger en el canal 1");
                                for (int k = 0; k < 101; k++) {
                                    if (datos[k] < 0) {

                                        series4Numbers.set(k, 0);

                                    } else {

                                        series4Numbers.set(k, 0);
                                    }
                                }
                            }
                            if((char)datos[102]=='2') {
                                for (int k = 0; k < 101; k++) {
                                    if (datos[k] < 0) {

                                        series5Numbers.set(k, 0);

                                    } else {

                                        series5Numbers.set(k, 0);
                                    }
                                }
                            }

                        }
                        else {
                            if ((char) datos[102] == 'x')
                            {
                                for (int k = 0; k < 101; k++) {
                                    if (datos[k] < 0) {
                                        if ((char) datos[102] == 'x') {
                                            series4Numbers.set(k, (Number) ((datos[k] + 256) * 5.0d / 255.0d));
                                        }

                                    } else {
                                        if ((char) datos[102] == 'x') {
                                            series4Numbers.set(k, (Number) ((datos[k]) * 5.0d / 255.0d));
                                        }
                                    }
                                }
                            }
                            if ((char) datos[102] == '2')
                            {

                                for (int k = 0; k < 101; k++) {
                                    if (datos[k] < 0) {

                                        series5Numbers.set(k, (Number) ((datos[k] + 256) * 5.0d / 255.0d));


                                    } else
                                    {

                                        series5Numbers.set(k, (Number) ((datos[k]) * 5.0d / 255.0d));

                                    }
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

        for(int j=0;j<101;j++){
            array.add(Math.sin(0.6*3.14*(0.1*j))*2.5+2.5);
        }

        return array;
    }
    public ArrayList<Number> crearSerieY2()
    {

        ArrayList<Number> array= new ArrayList<Number>();

        for(int j=0;j<101;j++){
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
    public ArrayList<Number> crearSerieX()
    {
        ArrayList<Number> array= new ArrayList<Number>();

        for(int g=0;g<101;g++){
            array.add(g*0.1);
        }
        return array;
    }
    public SimpleXYSeries desplazar(SimpleXYSeries serie,double y) {
        for (int i=0; i<100;i++){
            serie.setY(serie.getY(i+1),i);
        }
        serie.setY(y,100);
        return serie;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId){

            case R.id.rbc1:
                if(series1Removida==true){
                    mySimpleXYPlot.addSeries(series1,series1Format);
                    series1Removida=false;
                }
                mySimpleXYPlot.removeSeries(series2);
                series2Removida=true;
                mySimpleXYPlot.invalidate();

                break;
            case R.id.rbc2:

                mySimpleXYPlot.removeSeries(series1);
                if(series2Removida==true){
                    mySimpleXYPlot.addSeries(series2,series2Format);
                    series2Removida=false;
                }
                mySimpleXYPlot.invalidate();
                series1Removida=true;
                break;
            case R.id.rbd:
                if(series2Removida==true){
                    mySimpleXYPlot.addSeries(series2,series2Format);
                    series2Removida=false;
                }
                if(series1Removida==true){
                    mySimpleXYPlot.addSeries(series1,series1Format);
                    series1Removida=false;
                }
                mySimpleXYPlot.invalidate();

                break;
        }
    }

}
