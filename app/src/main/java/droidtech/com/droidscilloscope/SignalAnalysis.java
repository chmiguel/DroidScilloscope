package droidtech.com.droidscilloscope;

/**
 * Created by temporal on 23/10/2016.
 */

public class SignalAnalysis {
    public SignalAnalysis() {

    }
    static int getFrecuency(byte datos[])
    {
        int size= datos.length;
        double max1=0;
        double max2;
        boolean subida=false;
        for(int i=0;i<size;i++){
            if(datos[i]>max1) {
                max1= datos[i];
            }

            if(i<size-2)
            {
                if((datos[i]+2)<(datos[i+1]))
                {
                    subida=true;

                }
            }

        }

        return 0;
    }

}
