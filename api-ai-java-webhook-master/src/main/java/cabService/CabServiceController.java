package cabService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.json.*;
import java.sql.*;

import javax.net.ssl.HttpsURLConnection;

import java.io.*;
import java.net.*;

@Controller
@RequestMapping("/webhook")
public class CabServiceController {
	String driverName = "com.mysql.jdbc.Driver";
	String connectionURL = "jdbc:mysql://us-cdbr-iron-east-03.cleardb.net:3306/heroku_b08396768b01447";
	String userName = "bb3c231aece699";
	String password = "9d7a7614";

	@RequestMapping(method = RequestMethod.POST)
	public @ResponseBody WebhookResponse webhook(@RequestBody String obj)
			throws JSONException, ClassNotFoundException, SQLException, IOException {
		System.out.println("**********Entered into the webhook");
		System.out.println("object is" + obj);
		Class.forName(driverName);
		Connection conn = DriverManager.getConnection(connectionURL, userName, password);

		JSONObject result = new JSONObject(obj).getJSONObject("result");
		String empId = getEmployeeId(result);
		String time = getTime(result);
		String action = result.getString("action");
		System.out.println("action is:" + action);
		
		if (action.equals("EnterTimeIntoCabRequest")) {
			System.out.println("************Time is " + time);
			if (time.equals("")) {
				return new WebhookResponse("Can you please provide the time", 
					"Can you please provide the time");
			}

			// Add data to the Request table `cabrequest`
			enterTimeintoCabRequest(time, conn);
		}

		else if (action.equals("EnterIdIntoCabRequest")) {
			System.out.println("************EmployeeId is " + empId);
			if (checkUserExists("kh" + empId, conn) == 1) {
				deleteTheRecord(conn);
				return new WebhookResponse("Sorry you are already registered user",
					"Sorry you are already registered user");
			}
			else if (checkEmpId("kh" + empId, conn) == 0) {
				deleteTheRecord(conn);
				return new WebhookResponse("Employee Id provided is wrong, can you please provided the valid Id",
					"Employee Id provided is wrong, can you please provided the valid Id");
			}
			// Add data to the Request table `cabrequest`
			enterIdintoCabRequest("kh" + empId, conn);
		}

		else if (action.equals("GetCurrentLocation")) {
			String location = getLocation();
			System.out.println("***********Location is " + location);
		}

		else if (action.equals("GetTheCabNumber")) {
			System.out.println("get the cab number");
			String cabNumber = getCabNumber(empId, conn);
		
			return new WebhookResponse("Your cab number is " + cabNumber, 
				"Your cab number is " + cabNumber);
		}
		
		else if (action.equals("GetMyCabMates")) {
			System.out.println("Get the Cab mates");
			String mates = getCabMates(empId, conn);
			return new WebhookResponse("Your cab mates are" + mates, "Your cab mates are" + mates);
		}

		return new WebhookResponse("", "");
	}

// *************************** HELPER FUNCTIONS *********************************************************
	public void enterIdintoCabRequest(String empId, Connection conn) throws SQLException, ClassNotFoundException {
		Statement stat = conn.createStatement();
		String query = "update cabrequest set emp_id='" + empId + "' where emp_id = 'kh0000'";
		stat.executeUpdate(query);
	}

	public void enterTimeintoCabRequest(String time, Connection conn) throws SQLException, ClassNotFoundException {
		Statement stat = conn.createStatement();
		String query = "insert into cabrequest values('kh0000','" + time + "')";
		stat.executeUpdate(query);
	}

	public String getLocation() throws IOException, JSONException {
		String url = "http://kh2167.kitspl.com:9191/services/GoogleGeoLocation/GetLocation";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		// String payload = "{ \"homeMobileCountryCode\": 404,
		// \"homeMobileNetworkCode\": 78, \"radioType\": \"gsm\", \"carrier\":
		// \"idea\", \"considerIp\": \"true\" }";
		// add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(wr));
		// pw.write(payload);
		pw.close();

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}

		System.out.println("Response:" + response);

		/*
		 * JSONObject responseObject = new JSONObject(response); String lat =
		 * responseObject.getJSONObject("location").getString("lat"); String lng
		 * = responseObject.getJSONObject("location").getString("lat");
		 * System.out.println("latitude is" + lat + "longitude is" + lng);
		 */

		in.close();

		// print result
		System.out.println("The response for the google api is:" + response.toString());
		return response.toString();
	}

	public int checkEmpId(String empId, Connection conn) throws SQLException {
		int check = 0;
		Statement stat = conn.createStatement();
		String query = "select emp_id from EmployeeDetails";
		ResultSet rs = stat.executeQuery(query);
		while (rs.next()) {
			String responseEmpId = rs.getString("emp_id");
			System.out.println(responseEmpId);
			if (responseEmpId.equals(empId)) {
				check = 1;
			}
		}
		System.out.println("check returned " + check);
		return check;
	}

	public String getCabNumber(String empId, Connection conn) throws SQLException {
		System.out.println("Getting the details of " + empId);
		String query = "select * from cabassignment where emp_id = 'kh" + empId + "'";
		Statement stat = conn.createStatement();
		ResultSet rs = stat.executeQuery(query);
		
		String cabNumber = "";
		while(rs.next()) {
			cabNumber = rs.getString("cab_no");
		}
		return cabNumber;
	}

	public String getCabMates(String empId, Connection conn) throws SQLException {
		String cabNumber = getCabNumber(empId,conn);
		String query = "select * from cabassignment where cab_no = '"+ cabNumber +
			"' and emp_id <> '"+ empId +"'";
		Statement stat = conn.createStatement();
		ResultSet rs = stat.executeQuery(query);
		
		StringBuffer sb = new StringBuffer("");
		while(rs.next()) {
			String id = rs.getString("emp_id");
			String anotherQuery = "select emp_firstname from employeedetails where emp_id = '"+ id +"'";
			stat = conn.createStatement();
			ResultSet r = stat.executeQuery(anotherQuery);
			while(r.next()) {
				sb = sb.append(new StringBuffer(r.getString("emp_firstname")));
			}
			sb = sb.append(", ");
		}
		return sb.toString();
	}

	public int checkUserExists(String empId, Connection conn) throws SQLException {
		int check = 0;
		Statement stat = conn.createStatement();
		String query = "select emp_id from cabrequest";
		ResultSet rs = stat.executeQuery(query);
		while (rs.next()) {
			String responseEmpId = rs.getString("emp_id");
			System.out.println(responseEmpId);
			if (responseEmpId.equals(empId)) {
				check = 1;
			}
		}
		return check;
	}

	public void deleteTheRecord(Connection conn) throws SQLException {
		String query = "delete from cabrequest where emp_id = 'kh0000'";
		Statement stat = conn.createStatement();
		JSONObject obj = new JSONObject();
		stat.executeUpdate(query);
		System.out.println("Delete the records!!!");
	}
	
	public String getEmployeeId(JSONObject result) throws JSONException {
		String empId = "";
		empId = result.getJSONObject("parameters").optString("number");
		if (empId.equals("")) {
			JSONObject parameters = ((JSONObject) result.getJSONArray("contexts").get(0)).getJSONObject("parameters");
			empId = parameters.getString("number");
		}
		return empId;
	}
	
	public String getTime(JSONObject result) throws JSONException {
		String time = "";
		time = result.getJSONObject("parameters").optString("date-time");
		int i=0;
		while (time.equals("")) {
			JSONObject parameters = ((JSONObject) result.getJSONArray("contexts").get(i)).getJSONObject("parameters");
			time = parameters.optString("date-time");
			i++;
		}
		return time;
	}
}
