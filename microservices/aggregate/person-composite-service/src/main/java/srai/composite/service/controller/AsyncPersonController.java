package srai.composite.service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import srai.common.micro.service.model.Person;
import srai.common.micro.service.model.Recommendation;
import srai.composite.service.model.PersonComposite;
import srai.composite.service.service.AsyncPersonService;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/async")
public class AsyncPersonController {

  private static final Logger LOG = LoggerFactory.getLogger(AsyncPersonController.class);

  @Autowired
  AsyncPersonService asyncPersonService;

  @RequestMapping("/{personId}")
  public ResponseEntity<PersonComposite> getProduct(@PathVariable int personId) throws InterruptedException, ExecutionException {

    // FIXME. Enabling this will result in a ParameterizedTypeImpl exception and appears to be a limitation of Hystrix futures and
    // parameterized types
    //Future<ArrayList<String>> test = asyncPersonService.getStringList(1);

    Future<Person> personResult = asyncPersonService.getPersonAsync(personId);
    Future<Recommendation[]> recommendationPersonResult = asyncPersonService.getPersonRecommendationsAsync(personId);
    Future<Recommendation[]> recommendationProductResult = asyncPersonService.getProductRecommendationsAsync(personId);

    while(!(personResult.isDone() && recommendationPersonResult.isDone() && recommendationProductResult.isDone() ) ) {
      Thread.sleep(1);
    }

    if (personResult.get()== null) {
      return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
    }

    Person person = personResult.get();
    Recommendation[] personRecommendations = recommendationPersonResult.get();
    Recommendation[] productRecommendations = recommendationProductResult.get();

    PersonComposite result = new PersonComposite();
    result.setPerson(person);
    result.setPersonRecommendations(Arrays.asList(personRecommendations));
    result.setProductRecommendations(Arrays.asList(productRecommendations));
    //  return new ResponseEntity<PersonComposite>(result, personResult.get().getStatusCode());
    return new ResponseEntity<PersonComposite>(result, HttpStatus.ACCEPTED);
  }

}
