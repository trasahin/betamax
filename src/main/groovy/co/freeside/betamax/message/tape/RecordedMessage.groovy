package co.freeside.betamax.message.tape

import co.freeside.betamax.encoding.AbstractEncoder
import co.freeside.betamax.encoding.DeflateEncoder
import co.freeside.betamax.encoding.GzipEncoder
import co.freeside.betamax.encoding.NoOpEncoder
import co.freeside.betamax.message.AbstractMessage
import co.freeside.betamax.message.Message
import org.apache.http.HttpHeaders

abstract class RecordedMessage extends AbstractMessage implements Message {

	Map<String, String> headers = [:]
	def body

	final void addHeader(String name, String value) {
		if (headers[name]) {
			headers[name] = "${headers[name]}, $value"
		} else {
			headers[name] = value
		}
	}

    public final boolean hasBody() {
        return body != null;
    }

    @Override
    public final Reader getBodyAsText() {
        String string;
        if (hasBody())
            string = body instanceof String ? (String)body : getEncoder().decode(getBodyAsBinary(), getCharset());
        else
            string = "";

        return new StringReader(string);
    }


    public final InputStream getBodyAsBinary() {
        byte[] bytes;
        if (hasBody())
            bytes = (body instanceof String ? getEncoder().encode((String) body, getCharset()) : (byte[])body);
        else
            bytes = new byte[0];

        return new ByteArrayInputStream(bytes);
    }

	private AbstractEncoder getEncoder() {
		switch (getHeader(HttpHeaders.CONTENT_ENCODING)) {
			case 'gzip':
				return new GzipEncoder()
			case 'deflate':
				return new DeflateEncoder()
			default:
				return new NoOpEncoder()
		}
	}
}
