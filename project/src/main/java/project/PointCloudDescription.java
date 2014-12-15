package project;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class PointCloudDescription {

	public static String descriptionHtmlString(String serverName, int i){
		//String pointcloudlist = new String();
		try{
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Query query = new Query("PointCloudMeta");
			Iterable<Entity> pointCloud2 = datastore.prepare(query).asIterable();
			String pointcloudlist = new String("<table>");
			for(Entity meta: pointCloud2)
			{

				Key statusKey = KeyFactory.createKey("PointCloudConversionStatus",
						meta.getKey().getName());



				query = new Query(statusKey);
				Entity status = datastore.prepare(query).asSingleEntity();

				if(status == null){
					pointcloudlist = pointcloudlist +
							"<tr><td>" +meta.getProperty("name") + "</td><td>Processing</td></tr>";
					continue;
				}
				if((Long)status.getProperty("failed") == 1)
				{
					pointcloudlist = pointcloudlist +
							"<tr><td>" +meta.getProperty("name") + "</td><td>Processing Failed</td></tr>";
					continue;				
				}



				Filter filter =
						new FilterPredicate("cloud",
								FilterOperator.EQUAL,
								meta.getKey().getName());

				Query fieldQuery = new Query("PointField").setFilter(filter).addSort("idx");

				Iterable<Entity> fields = datastore.prepare(fieldQuery).asIterable();


				pointcloudlist = pointcloudlist +
						"<tr><td><a target=\"viewer\" href=\"http://www.pointclouds.org/assets/viewer/pcl_viewer.html?load=http://"
						+ serverName + ":" + i + "/GetPCD/" + meta.getKey().getName() + "\" >"
						+ (String) meta.getProperty("name")+ "</a></td>";
				for(Entity field: fields)
				{
					pointcloudlist = pointcloudlist + "<td>" + (String) field.getProperty("name");
				}
				pointcloudlist = pointcloudlist + "</tr>";
			}
			pointcloudlist = pointcloudlist + "</table>";
			return pointcloudlist;
		}
		catch(Exception e)
		{
			return "<p>Can't generate pointcloud list </p>";
			
		}
	}

}
