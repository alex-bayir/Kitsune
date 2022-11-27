package org.alex.kitsune.commons;

import java.io.IOException;
public class HttpStatusException extends IOException {
    private final int code;
    private final String description;
    private final String url;

    public HttpStatusException(int code, String message, String url) {
        super(message(code,message,url));
        this.code=code;
        this.description=message;
        this.url=url;
    }

    public HttpStatusException(int code, String url){this(code, message(code),url);}

    public int code(){return code;}
    public String url(){return url;}
    public String description(){return description;}
    private static String message(String format,int code,String description,String url){return String.format(format,code,description,url);}
    private static String message(int code,String description,String url){return message("%d - %s\nURL:%s",code,description,url);}
    public String message(String format){return message(format,code,description,url);}
    public String message(){return message(code,description,url);}

    public static String message(int code){
        return switch (code) {
            case 100 -> "Continue";
            case 101 -> "Switching Protocols";
            case 102 -> "Processing";
            case 103 -> "Early Hints";
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 203 -> "Non-Authoritative Information";
            case 204 -> "No Content";
            case 205 -> "Reset Content";
            case 206 -> "Partial Content";
            case 207 -> "Multi-Status";
            case 208 -> "Already Reported";
            case 226 -> "IM Used";
            case 300 -> "Multiple Choices";
            case 301 -> "Moved Permanently";
            case 302 -> "Moved Temporarily";
            case 303 -> "See Other";
            case 304 -> "Not Modified";
            case 305 -> "Use Proxy";
            case 307 -> "Temporary Redirect";
            case 308 -> "Permanent Redirect";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 402 -> "Payment Required";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 406 -> "Not Acceptable";
            case 407 -> "Proxy Authentication Required";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 410 -> "Gone";
            case 411 -> "Length Required";
            case 412 -> "Precondition Failed";
            case 413 -> "Payload Too Large";
            case 414 -> "URI Too Long";
            case 415 -> "Unsupported Media Type";
            case 423 -> "Locked";
            case 429 -> "Too Many Requests";
            case 451 -> "Unavailable For Legal Reasons";
            case 499 -> "Client Closed Request";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            case 505 -> "HTTP Version Not Supported";
            case 511 -> "Network Authentication Required";
            case 520 -> "Unknown Error";
            case 521 -> "Web Server Is Down";
            case 522 -> "Connection Timed Out ";
            case 523 -> "Origin Is Unreachable";
            case 524 -> "A Timeout Occurred";
            case 525 -> "SSL Handshake Failed";
            case 526 -> "Invalid SSL Certificate";
            default -> "HTTP error fetching URL";
        };
    }
}
