package com.philippkutsch.tuchain.chain.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ChainUtils {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(byte[].class, new Base64TypeAdapter())
            .create();
    public static final Gson GSON_BEAUTY = new GsonBuilder()
            .registerTypeAdapter(byte[].class, new Base64TypeAdapter())
            .setPrettyPrinting()
            .create();

    @Nonnull
    public static String encodeToString(@Nonnull Object object) {
        return ChainUtils.GSON.toJson(object);
    }

    @Nonnull
    public static String encodeToBeautyString(@Nonnull Object object) {
        return ChainUtils.GSON_BEAUTY.toJson(object);
    }

    @Nonnull
    public static <T> T decodeFromString(@Nonnull String data, @Nonnull Class<T> clazz) {
        return ChainUtils.GSON.fromJson(data, clazz);
    }

    @Nonnull
    public static byte[] encodeToBytes(@Nonnull Object object) {
        return encodeToString(object).getBytes(StandardCharsets.UTF_8);
    }

    @Nonnull
    public static <T> T decodeFromBytes(@Nonnull byte[] data, @Nonnull Class<T> clazz) {
        return decodeFromString(new String(data, StandardCharsets.UTF_8), clazz);
    }

    @Nonnull
    public static byte[] bytesFromBase64(@Nonnull String data) {
        return Base64.getDecoder().decode(data);
    }

    @Nonnull
    public static String bytesToBase64(@Nonnull byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
