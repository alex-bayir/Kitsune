package org.alex.kitsune.commons;

import java.io.IOException;
public class HttpStatusException extends IOException {
    private final int code;
    private final String code_description;
    private final String url;

    public HttpStatusException(String message, int code, String url) {
        super(message+". Status="+code+", URL=["+url+"]");
        this.code=code;
        this.url=url;
        this.code_description=message;
    }

    public HttpStatusException(int code, String url){this(message(code),code, url);}

    public int code(){return code;}
    public String url(){return url;}
    public String description(){return code_description;}
    public String description_code(String format){return String.format(format,code,code_description);}
    public String description_code(){return description_code("%d (%s)");}

    public static String message(int code){
        switch (code){
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Payment Required";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 406: return "Not Acceptable";
            case 407: return "Proxy Authentication Required";
            case 408: return "Request Timeout";
            case 409: return "Conflict";
            case 410: return "Gone";
            case 411: return "Length Required";
            case 412: return "Precondition Failed";
            case 413: return "Payload Too Large";
            case 414: return "URI Too Long";
            case 415: return "Unsupported Media Type";
            case 423: return "Locked";
            case 429: return "Too Many Requests";
            case 451: return "Unavailable For Legal Reasons";
            case 499: return "Client Closed Request";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            case 505: return "HTTP Version Not Supported";
            case 511: return "Network Authentication Required";
            case 520: return "Unknown Error";
            case 521: return "Web Server Is Down";
            case 522: return "Connection Timed Out ";
            case 523: return "Origin Is Unreachable";
            case 524: return "A Timeout Occurred";
            case 525: return "SSL Handshake Failed";
            case 526: return "Invalid SSL Certificate";
            default: return "HTTP error fetching URL";
        }
    }
}
