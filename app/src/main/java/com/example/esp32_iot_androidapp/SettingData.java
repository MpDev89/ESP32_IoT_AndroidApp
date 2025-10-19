package com.example.esp32_iot_androidapp;

import static java.util.Arrays.copyOf;

import java.math.BigInteger;

public class SettingData {
    /* - Get String from array byte */
    public String byteToString(byte[] num) {

        byte[] num_str = copyOf(num, num.length);
        byte temp =0;

        for(int i = 0; i < num_str.length / 2; i++)
        {
            temp = num_str[i];
            num_str[i] = num_str[num_str.length - i - 1];
            num_str[num_str.length - i - 1] = temp;
        }
        BigInteger one = new BigInteger(num_str);

        return one.toString();

    }

    /* - Get Int from array byte */
    public int getData(byte[] num) {

        byte[] num_int = copyOf(num, num.length);
        byte temp =0;

        for(int i = 0; i < num_int.length / 2; i++)
        {
            temp = num_int[i];
            num_int[i] = num_int[num_int.length - i - 1];
            num_int[num_int.length - i - 1] = temp;
        }
        BigInteger one = new BigInteger(num_int);
        int data_out = one.intValue();
        return data_out;
    }
    public static int convertString2Int(String num) {

        int data_out = 0;
        try {
            data_out = Integer.parseInt(num);
        }
        catch (NumberFormatException e)
        {
            data_out = 0;
        }

        return data_out;
    }

    public static int convertHexString2Int(String num) {

        int data_out = 0;
        try {
            data_out = Integer.parseInt(num,16);
        }
        catch (NumberFormatException e)
        {
        }
        return data_out;
    }

    public static int convertBleData(String data) {

        StringBuilder data_out = new StringBuilder();

        for (int i = data.length() -1 ; i >= 0; i= i - 2){
            data_out.append(data.substring(i - 1, i + 1));
        }

        return convertHexString2Int(data_out.toString());
    }
}
