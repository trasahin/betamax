package co.freeside.betamax.compatibility

import co.freeside.betamax.*
import co.freeside.betamax.httpclient.*
import co.freeside.betamax.proxy.jetty.SimpleServer
import co.freeside.betamax.util.server.*
import org.apache.http.HttpHost
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.*
import org.junit.Rule
import spock.lang.*
import static co.freeside.betamax.TapeMode.WRITE_ONLY
import static co.freeside.betamax.util.FileUtils.newTempDir
import static java.net.HttpURLConnection.HTTP_OK
import static org.apache.http.HttpHeaders.VIA
import static org.apache.http.HttpStatus.SC_OK
import static org.apache.http.conn.params.ConnRoutePNames.DEFAULT_PROXY

class HttpClientSpec extends Specification {

	@AutoCleanup('deleteDir') File tapeRoot = newTempDir('tapes')
	@Rule ProxyRecorder recorder = new ProxyRecorder(tapeRoot: tapeRoot, defaultMode: WRITE_ONLY, sslSupport: true)
	@Shared @AutoCleanup('stop') SimpleServer endpoint = new SimpleServer()
	@Shared @AutoCleanup('stop') SimpleServer httpsEndpoint = new SimpleSecureServer(5001)

	void setupSpec() {
		endpoint.start(EchoHandler)
		httpsEndpoint.start(HelloHandler)
	}

	@Timeout(10)
	@Betamax(tape = 'http client spec', mode = TapeMode.READ_WRITE)
	void 'proxy intercepts HTTPClient connections when using ProxySelectorRoutePlanner'() {
		given:
		def http = new DefaultHttpClient()
		BetamaxRoutePlanner.configure(http)

		when:
		def request = new HttpGet(endpoint.url)
		def response = http.execute(request)

		then:
		response.statusLine.statusCode == HTTP_OK
		response.getFirstHeader(VIA)?.value == 'Betamax'
	}

	@Timeout(10)
	@Betamax(tape = 'http client spec', mode = TapeMode.READ_WRITE)
	void 'proxy intercepts HTTPClient connections when explicitly told to'() {
		given:
		def http = new DefaultHttpClient()
		http.params.setParameter(DEFAULT_PROXY, new HttpHost('localhost', recorder.proxyPort, 'http'))

		when:
		def request = new HttpGet(endpoint.url)
		def response = http.execute(request)

		then:
		response.statusLine.statusCode == HTTP_OK
		response.getFirstHeader(VIA)?.value == 'Betamax'
	}

	@Timeout(10)
	@Betamax(tape = 'http client spec', mode = TapeMode.READ_WRITE)
	void 'proxy automatically intercepts SystemDefaultHttpClient connections'() {
		given:
		def http = new SystemDefaultHttpClient()

		when:
		def request = new HttpGet(endpoint.url)
		def response = http.execute(request)

		then:
		response.statusLine.statusCode == HTTP_OK
		response.getFirstHeader(VIA)?.value == 'Betamax'
	}

	@Ignore("until HTTPS support implemented")
	@Betamax(tape = 'http client spec')
	void 'proxy can intercept HTTPS requests'() {
		given:
		def http = new DefaultHttpClient()
		BetamaxRoutePlanner.configure(http)
		BetamaxHttpsSupport.configure(http)

		when: 'an HTTPS request is made'
		def request = new HttpGet(httpsEndpoint.url)
		def response = http.execute(request)

		and: 'we read the response body'
		def responseBytes = new ByteArrayOutputStream()
		response.entity.writeTo(responseBytes)
		def responseString = responseBytes.toString('UTF-8')

		then: 'the request is intercepted by the proxy'
		response.statusLine.statusCode == SC_OK
		response.getFirstHeader(VIA)?.value == 'Betamax'

		and: 'the response is decoded'
		responseString == 'Hello World!'
	}
}
