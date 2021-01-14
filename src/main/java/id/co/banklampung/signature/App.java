package id.co.banklampung.signature;

import id.co.banklampung.signature.helper.HmacHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class App {

    public static final String SECRET_KEY = "1234567890";
    public static final String ACCESS_TOKEN = "abc";
    public static final String SEPARATOR = ":";

    public static void main(String[] args) {
        String httpPostRequest = buildHttpPostRequest();

        byte[] hmacSha256 = HmacHelper.toHmacSha256(toBytes(SECRET_KEY), toBytes(httpPostRequest));

        String printBase64Binary = DatatypeConverter.printBase64Binary(hmacSha256);
        String printHexBinary = DatatypeConverter.printHexBinary(hmacSha256).toLowerCase();

        byte[] parseBase64Binary = DatatypeConverter.parseBase64Binary(printBase64Binary);
        byte[] parseHexBinary = DatatypeConverter.parseHexBinary(printHexBinary);

        System.out.println("string original: " + httpPostRequest);
        System.out.println("string base64: " + printBase64Binary);
        System.out.println("string hexa: " + printHexBinary);
        System.out.println("equal base64: " + MessageDigest.isEqual(hmacSha256, parseBase64Binary));
        System.out.println("equal hexa: " + MessageDigest.isEqual(hmacSha256, parseHexBinary));
    }

    static String buildHttpPostRequest() {
        final String body = "{\n" +
                "    \"inquiry_partner_id\": \"abcdefghijklmnopqrs\",\n" +
                "    \"bank_code\": \"121\",\n" +
                "    \"bank_account\": \"3800304236315\",\n" +
                "    \"user_id\": \"1\"\n" +
                "}";

        return buildHttpRequest("POST", "/inquiry", "", toBody(body));

    }

    static String buildHttpGetRequest() {
        return buildHttpRequest("GET", "/account", "code=12345678&name=WEDUS", "");
    }

    static String buildHttpRequest(String httpMethod, String path, String queryParams, String body) {
        final byte[] bodySha256 = HmacHelper.toSha256(toBytes(body));
        final String bodySha256Hexa = DatatypeConverter.printHexBinary(bodySha256).toLowerCase();

        return new StringBuilder()
                .append(httpMethod).append(SEPARATOR)
                .append(toRelativePath(path, queryParams)).append(SEPARATOR)
                .append(ACCESS_TOKEN).append(SEPARATOR)
                .append(bodySha256Hexa).append(SEPARATOR)
                .append(toTimestamp())
                .toString();
    }

    static String toRelativePath(String path, String queryParams) {
        return Optional.ofNullable(queryParams)
                .filter(StringUtils::isNotBlank)
                .map(App::toQueryParams)
                .map(val -> String.format("%s?%s", path, val))
                .orElse(path);
    }

    static String toQueryParams(String queryParams) {
        List<String> queries = Arrays.stream(queryParams.split("&"))
                .collect(Collectors.toList());

        queries.sort(String.CASE_INSENSITIVE_ORDER);

        return String.join("&", queries);
    }

    static String toBody(String body) {
        String cleanBody = body.replaceAll("\\r|\\n|\\t", "");
        return CaseUtils.toCamelCase(cleanBody, true, new char[]{' '});
    }

    static String toTimestamp() {
        Clock clock = Clock.system(ZoneId.of("Asia/Jakarta"));
        Instant instant = clock.instant();
        ZonedDateTime time = instant.atZone(clock.getZone());

        return time.toString().substring(0, 29);
    }

    static byte[] toBytes(String payload) {
        return payload.getBytes(StandardCharsets.UTF_8);
    }

}
