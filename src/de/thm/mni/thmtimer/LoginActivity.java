package de.thm.mni.thmtimer;

import java.util.Collections;

import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import de.thm.mni.thmtimer.util.AbstractAsyncActivity;
import de.thm.mni.thmtimer.util.StaticModuleData;
import de.thm.thmtimer.entities.User;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AbstractAsyncActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.loginactivity);

		// ONLY FOR STATIC DATA
		StaticModuleData.fillData();

		Button btnLogin = (Button) findViewById(R.id.btn_login);
		btnLogin.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {

				new FetchSecuredResourceTask().execute();
				// Intent intent = new Intent(LoginActivity.this,
				// ModuleListActivity.class);
				// startActivity(intent);
			}
		});
	}

	private void displayResponse(User response) {
		Toast.makeText(this, "Hallo " + response.getFirstName()	, Toast.LENGTH_LONG).show();
	}

	private class FetchSecuredResourceTask extends AsyncTask<Void, Void, User> {

		private String username;

		private String password;

		@Override
		protected void onPreExecute() {
			showLoadingProgressDialog();

			// build the message object
			EditText editText = (EditText) findViewById(R.id.user);
			this.username = editText.getText().toString();

			editText = (EditText) findViewById(R.id.password);
			this.password = editText.getText().toString();
		}

		@Override
		protected User doInBackground(Void... params) {
			
			final String url = getString(R.string.base_uri) + "/users/" + username;

			// Populate the HTTP Basic Authentitcation header with the username
			// and password
			HttpAuthentication authHeader = new HttpBasicAuthentication(username, password);
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.setAuthorization(authHeader);
			HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);

			// Create a new RestTemplate instance
			RestTemplate restTemplate = new RestTemplate();

			// Add the String message converter
			restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());

			try {
				// Make the network requesst
				Log.d(TAG, url);
				ResponseEntity<User> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, User.class);
			    return response.getBody();
			} catch (HttpClientErrorException e) {
				Log.e(TAG, e.getLocalizedMessage(), e);
				return null;
			} catch (ResourceAccessException e) {
				Log.e(TAG, e.getLocalizedMessage(), e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(User result) {
			dismissProgressDialog();
			displayResponse(result);
		}

	}

}
