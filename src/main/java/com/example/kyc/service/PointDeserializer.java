package com.example.kyc.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.IOException;

public class PointDeserializer extends JsonDeserializer<Point> {

    private static final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public Point deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        if (node.has("coordinates") && node.get("coordinates").isArray()) {
            JsonNode coords = node.get("coordinates");
            double lon = coords.get(0).asDouble();
            double lat = coords.get(1).asDouble();
            return factory.createPoint(new Coordinate(lon, lat));
        }
        return null;
    }
}