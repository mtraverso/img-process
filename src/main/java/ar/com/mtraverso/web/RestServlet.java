package ar.com.mtraverso.web;

import ar.com.mtraverso.WebCarCounter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Created by mtraverso on 11/2/16.
 */
@Path("/data")
public class RestServlet {

    Gson gson = new GsonBuilder().create();

    @GET
    @Path("/")
    public String getValues(){
        JsonObject object = new JsonObject();
        object.addProperty("decreasing",WebCarCounter.getInstance().getCarCount("decreasing"));
        object.addProperty("increasing",WebCarCounter.getInstance().getCarCount("increasing"));
        return gson.toJson(object);
    }

    @GET
    @Path("/{direction}")
    public String getValue(@PathParam("direction") String direction){
        JsonObject obj = new JsonObject();
        obj.addProperty(direction,WebCarCounter.getInstance().getCarCount(direction));
        return gson.toJson(obj);
    }
}
