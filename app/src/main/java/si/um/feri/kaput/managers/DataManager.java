package si.um.feri.kaput.managers;
import android.util.Log;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import si.um.feri.kaput.models.Location;
import si.um.feri.kaput.models.LocationUser;
import si.um.feri.kaput.models.Transaction;

//NOTE POMEMBNO: lokacije ki so naložene imajo vse podatke kot v databazi, v momentu poizvedbe,
// tako da podatki se spreminjajo, inflow, outflow, number of transactions, users,ž
// prepiši oziroma ponastavi glede na potrebe
public class DataManager {
    private final List<Location> FamilyLocations = new ArrayList<>();
    private final List<Location> UserLocations = new ArrayList<>();
    private final List<Pair<String, String>> familyMembers = new ArrayList<>(); // Pair<userId, username>
    private Pair<String, String> loggedInUser; // Pair<userId, username>

    // Getter
    public String getUserId() {
        return loggedInUser != null ? loggedInUser.first : null;
    }
    public String getUsername() {
        return loggedInUser != null ? loggedInUser.second : "";
    }
    public List<Pair<String, String>> getFamilyMembers() {
        return familyMembers;
    }
    public List<Location> getFamilyLocations() {
        return FamilyLocations;
    }
    public List<Location> getUserLocations() {
        return UserLocations;
    }
    // ------ Loading Data from JSON ------
    public void loadFamilyLocations(JSONObject data) {
        FamilyLocations.clear();
        JSONArray locations = data.optJSONArray("statistics");
        JSONArray membersJson = data.optJSONArray("familyMembers");
        assert membersJson != null;
        loadFamilyMembers(membersJson);
        Log.d("DataManager", "parsed locations: " + locations);

        if (locations == null) return;

        for (int i = 0; i < locations.length(); i++) {
            JSONObject locationJson = locations.optJSONObject(i);
            if (locationJson == null) continue;

            String locationId = locationJson.optString("_id", "");
            String locationName = locationJson.optString("name", "");
            String identifier = locationJson.optString("identifier", "");
            String address = locationJson.optString("address", "");
            int numbOfTrans = locationJson.optInt("number_of_transactions", 0);
            double total_inflow = locationJson.optDouble("total_inflow", 0.0);
            double total_outflow = locationJson.optDouble("total_outflow", 0.0);
            Double lat = locationJson.optDouble("lat", 0.0);
            Double lng = locationJson.optDouble("lng", 0.0);

            JSONArray usersJson = locationJson.optJSONArray("users");
            List<LocationUser> users = usersJson != null ? extractLocationUsers(usersJson) : new ArrayList<>();

            Location location = new Location(
                    locationId,
                    locationName,
                    identifier,
                    address,
                    numbOfTrans,
                    total_inflow,
                    total_outflow,
                    lat,
                    lng,
                    users
            );
            FamilyLocations.add(location);
        }
        Log.d("DataManager", "Loaded " + FamilyLocations.size() + " family locations.");
    }
    public void loadUserLocations(JSONObject data) {
        UserLocations.clear();
        JSONObject user = data.optJSONObject("user");
        if (user == null) return;

        JSONArray locations = user.optJSONArray("locations");
        if (locations == null) return;

        for (int i = 0; i < locations.length(); i++) {
            JSONObject locationJson = locations.optJSONObject(i);
            if (locationJson == null) continue;

            String locationId = locationJson.optString("_id", "");
            String locationName = locationJson.optString("name", "");
            String identifier = locationJson.optString("identifier", "");
            String address = locationJson.optString("address", "");
            int numbOfTrans = locationJson.optInt("numbOfTrans", 0);
            double total_inflow = locationJson.optDouble("total_inflow", 0.0);
            double total_outflow = locationJson.optDouble("total_outflow", 0.0);
            Double lat = locationJson.optDouble("lat", 0.0);
            Double lng = locationJson.optDouble("lng", 0.0);

            JSONArray usersJson = locationJson.optJSONArray("users");
            List<LocationUser> users = usersJson != null ? extractLocationUsers(usersJson) : new ArrayList<>();

            Location location = new Location(
                    locationId,
                    locationName,
                    identifier,
                    address,
                    numbOfTrans,
                    total_inflow,
                    total_outflow,
                    lat,
                    lng,
                    users
            );
            UserLocations.add(location);
        }
        Log.d("DataManager", "Loaded " + UserLocations.size() + " user locations.");
    }
    public List<LocationUser> extractLocationUsers(JSONArray usersJson) {
        List<LocationUser> users = new ArrayList<>();
        for (int j = 0; j < usersJson.length(); j++) {
            JSONObject userJson = usersJson.optJSONObject(j);
            if (userJson == null) continue;

            String userId = userJson.optString("userId", "");
            String username = userJson.optString("username", "");
            int numbOfTrans = userJson.optInt("numbOfTrans", 0);
            double inflow = userJson.optDouble("inflow", 0.0);
            double outflow = userJson.optDouble("outflow", 0.0);

            LocationUser user = new LocationUser(userId, username, numbOfTrans, inflow, outflow);
            users.add(user);
        }
        return users;
    }
    public void loadFamilyMembers(JSONArray membersJson) {
        for (int i = 0; i < membersJson.length(); i++) {
            JSONObject memberJson = membersJson.optJSONObject(i);
            if (memberJson == null) continue;
            String memberId = memberJson.optString("_id", "");
            String memberName = memberJson.optString("username", "");
            familyMembers.add(new Pair<>(memberId, memberName));
        }
    }
    public void setLoggedInUser(String userId, String username) {
        loggedInUser = new Pair<>(userId, username);
    }

    // ------ Utility Methods ------
    public Location getLocationByIdentifier(String identifier, List<Location> Locations) {
        for (Location loc : Locations) {
            if (loc.getIdentifier().equals(identifier)) {
                return loc;
            }
        }
        return null;
    }

}
