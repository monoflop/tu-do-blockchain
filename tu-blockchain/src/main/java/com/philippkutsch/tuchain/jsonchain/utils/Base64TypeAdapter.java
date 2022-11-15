package com.philippkutsch.tuchain.jsonchain.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Base64;

//Copied from https://gist.github.com/elucash/ee4d294cb0d920fc227d993aebf6d53c
public class Base64TypeAdapter extends TypeAdapter<byte[]> {
    @Override
    public void write(JsonWriter out, byte[] value) throws IOException {
        out.value(Base64.getEncoder().withoutPadding().encodeToString(value));
    }
    @Override
    public byte[] read(JsonReader in) throws IOException {
        return Base64.getDecoder().decode(in.nextString());
    }
}
