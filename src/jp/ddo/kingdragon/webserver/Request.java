package jp.ddo.kingdragon.webserver;

/**
 * HTTPリクエストを管理するクラス
 * @author 杉本祐介
 */
public class Request {
    // 変数の宣言
    /**
     * メソッド
     */
    private final String method;
    /**
     * 要求されたパス
     */
    private final String path;
    /**
     * HTTPのバージョン
     */
    private final String version;

    // コンストラクタ
    /**
     * HTTPリクエストからインスタンスを生成する
     * @param requestLine HTTPリクエスト
     */
    public Request(String requestLine) {
        String[] splittedLine = requestLine.split(" +");
        if (splittedLine.length == 3) {
            method = splittedLine[0];
            if (splittedLine[1].endsWith("/")) {
                path = splittedLine[1] + "index.html";
            }
            else {
                path = splittedLine[1];
            }
            version = splittedLine[2].replaceAll("HTTP/", "");
        }
        else {
            throw new IllegalArgumentException("Request : HTTPリクエストのフォーマットが正しくありません。");
        }
    }

    // アクセッサ
    /**
     * メソッドを返す
     * @return メソッド
     */
    public String getMethod() {
        return method;
    }
    /**
     * 要求されたパスを返す
     * @return 要求されたパス
     */
    public String getPath() {
        return path;
    }
    /**
     * HTTPのバージョンを返す
     * @return HTTPのバージョン
     */
    public String getVersion() {
        return version;
    }
}