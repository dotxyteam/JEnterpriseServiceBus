import java.net.URI;

public class TestURIs {

	public static void main(String[] args) throws Throwable {
		System.out.println(new URI(
				"http://www.java2s.com/example/getparent-final-uri-uri-f7d74.html")
						.resolve(new URI("relative.file")));
	}

}
