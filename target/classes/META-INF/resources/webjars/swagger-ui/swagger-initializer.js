window.onload = function() {debugger;
    const ui = SwaggerUIBundle({
        url: "/v3/api-docs",
        dom_id: '#swagger-ui',
        presets: [SwaggerUIBundle.presets.apis],
        layout: "BaseLayout",
        responseInterceptor: (req) => {debugger;
            console.log(req)
            // if calling login, capture token
            if (req.url.endsWith("/auth/authenticate")) {debugger;
                console.log(req.url)
                //req.responseInterceptor = (res) => {
                    try {debugger;
                        //const body = JSON.parse(res.text);
                        //console.log(body)
                        if (req.body?.data?.accessToken) {
                            ui.preauthorizeApiKey("bearerAuth", req.body?.data?.accessToken);
                        }
                    } catch (e) { console.error("JWT auto-apply failed", e); }
                    //return res;
                //}
            }
            return req;
        }
    });
}
