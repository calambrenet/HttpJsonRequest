HttpJsonRequest
===============

HttpJsonRequest es una simple clase para realizar llamadas http (POST, GET) usando JSON para Android.
Permite llamadas cifradas usando SSL. Para eso hay que crear un fichero como almacén de claves.

    InputStream in = context.getResources().openRawResource(R.raw.mykeystore);

E indicar la contraseña:

    keyStore.load(in, PASSWORD.toCharArray());

USO
---
Hay que indicar el tipo de retorno. En el constructor tenemos como opciones el contexto de la aplicación, la url de la llamada, el tipo de llamada (POST/GET), un String con la llamada json (opcional) y si es una llamada ssl.
Tenemos dos callbacks: onDone() y onFail().
Al ejecutar exec() ejecutamos la llamada. 
Lo ideal es meterla en un asyncTask y llamarlo en otro hilo.

    HttpJsonRequest<type> post = new HttpJsonRequest<type>(activity.getApplicationContext(), URL, HttpJsonRequest.POST, JSON_POST, true) {
    	@Override
    	public type onDone(JSONObject json_response, int status_code) {
    		// TODO Auto-generated method stub
    		return false;
    	}
    
    	@Override
    	public type onFail(int status_code) {
    		// TODO Auto-generated method stub
    		return null;
    	}
    };
    	
    try {
        type = post.exec();			
    } catch (Exception e) {
    	// TODO Auto-generated catch block
    	e.printStackTrace();
    }		

Es un buen punto de partida para crear una clase que nos sirva para automatizar este tipo de llamadas.
