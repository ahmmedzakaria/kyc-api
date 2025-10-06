package com.example.kyc.appconfigmodule.filters;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream capture;
    private ServletOutputStream output;
    private PrintWriter writer;

    public CachedBodyHttpServletResponse(HttpServletResponse response) {
        super(response);
        capture = new ByteArrayOutputStream(response.getBufferSize());
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) throw new IllegalStateException("Writer already in use");
        if (output == null) {
            output = new ServletOutputStream() {
                @Override
                public void write(int b) {
                    capture.write(b);
                }
                @Override
                public boolean isReady() { return true; }

                @Override
                public void setWriteListener(WriteListener listener) {

                }

            };
        }
        return output;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (output != null) throw new IllegalStateException("OutputStream already in use");
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(capture, getCharacterEncoding()));
        }
        return writer;
    }

    public byte[] getCapturedBody() {
        if (writer != null) writer.flush();
        return capture.toByteArray();
    }
}

