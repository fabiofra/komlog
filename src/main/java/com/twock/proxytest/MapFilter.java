package com.twock.proxytest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.twock.proxytest.map.MapTile;
import com.twock.proxytest.map.JsonMapReader;
import com.twock.proxytest.map.ParsedJsonMapResponse;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.littleshoot.proxy.HttpFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chris Pearson
 */
@Lazy
@Repository
@Transactional
public class MapFilter implements HttpFilter {
  public static final Charset UTF_8 = Charset.forName("UTF8");
  private static final Logger log = LoggerFactory.getLogger(MapFilter.class);
  @PersistenceContext
  private EntityManager entityManager;
  @Inject
  private JsonMapReader mapReader;

  @Override
  public HttpResponse filterResponse(HttpRequest request, HttpResponse response) {
    String responseString = response.getContent().toString(UTF_8);
    log.debug("Filtering {} {} {} {}", request.getMethod(), request.getUri(), request.getHeaders(), responseString);
      ParsedJsonMapResponse mapTiles = mapReader.parseJson(responseString);
      for(Map.Entry<String, MapTile> entry : mapTiles.getData().entrySet()) {
        entityManager.merge(entry.getValue());
      }
      entityManager.flush();
      log.debug("Parsed response: {}", mapTiles);
    return response;
  }

  @Override
  public int getMaxResponseSize() {
    return 1048576 /*1 MB*/;
  }

  @Override
  public boolean filterResponses(HttpRequest httpRequest) {
    boolean filter = HttpMethod.POST.equals(httpRequest.getMethod()) && httpRequest.getUri().endsWith("/fetchMapTiles.php");
    log.debug("{} {} to {}", filter ? "Filtering" : "Not filtering", httpRequest.getMethod().toString(), httpRequest.getUri());
    return filter;
  }
}