package com.example.kyc.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.locationtech.jts.geom.Point;

import java.io.IOException;

public class PointSerializer extends StdSerializer<Point> {

    private static final long serialVersionUID = 1L;

    public PointSerializer() {
        super(Point.class);
    }

    @Override
    public void serialize(Point value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeStringField("type", "Point");
        gen.writeArrayFieldStart("coordinates");
        gen.writeNumber(value.getX());
        gen.writeNumber(value.getY());
        gen.writeEndArray();
        gen.writeEndObject();
    }
}
