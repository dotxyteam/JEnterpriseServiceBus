package my.client;

import my.ApiClient;

import my.client.Pet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-07-23T20:09:45.240+02:00[Europe/Paris]")
public class PetsApi {
    private ApiClient apiClient;

    public PetsApi() {
        this(new ApiClient());
    }

    public PetsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Create a pet
     * 
     * <p><b>201</b> - Null response
     * <p><b>0</b> - unexpected error
     * @param pet  (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void createPets(Pet pet) throws RestClientException {
        createPetsWithHttpInfo(pet);
    }

    /**
     * Create a pet
     * 
     * <p><b>201</b> - Null response
     * <p><b>0</b> - unexpected error
     * @param pet  (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> createPetsWithHttpInfo(Pet pet) throws RestClientException {
        Object localVarPostBody = pet;
        
        // verify the required parameter 'pet' is set
        if (pet == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'pet' when calling createPets");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/pets", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * List all pets
     * 
     * <p><b>200</b> - A paged array of pets
     * <p><b>0</b> - unexpected error
     * @param limit How many items to return at one time (max 100) (optional)
     * @return List&lt;Pet&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public List<Pet> listPets(Integer limit) throws RestClientException {
        return listPetsWithHttpInfo(limit).getBody();
    }

    /**
     * List all pets
     * 
     * <p><b>200</b> - A paged array of pets
     * <p><b>0</b> - unexpected error
     * @param limit How many items to return at one time (max 100) (optional)
     * @return ResponseEntity&lt;List&lt;Pet&gt;&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Pet>> listPetsWithHttpInfo(Integer limit) throws RestClientException {
        Object localVarPostBody = null;
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<List<Pet>> localReturnType = new ParameterizedTypeReference<List<Pet>>() {};
        return apiClient.invokeAPI("/pets", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Info for a specific pet
     * 
     * <p><b>200</b> - Expected response to a valid request
     * <p><b>0</b> - unexpected error
     * @param petId The id of the pet to retrieve (required)
     * @return Pet
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public Pet showPetById(String petId) throws RestClientException {
        return showPetByIdWithHttpInfo(petId).getBody();
    }

    /**
     * Info for a specific pet
     * 
     * <p><b>200</b> - Expected response to a valid request
     * <p><b>0</b> - unexpected error
     * @param petId The id of the pet to retrieve (required)
     * @return ResponseEntity&lt;Pet&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Pet> showPetByIdWithHttpInfo(String petId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'petId' is set
        if (petId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'petId' when calling showPetById");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("petId", petId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Pet> localReturnType = new ParameterizedTypeReference<Pet>() {};
        return apiClient.invokeAPI("/pets/{petId}", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
}
