import requests
import json
import upload_rating
continue_reading = True
oldImageName = "name"

while continue_reading:

	url = "https://aiotlab.111mb.de/AndroidUploadImage/downloadImage.php"

	response = requests.request("GET", url)

	# parse response:
	y = json.loads(response.text)


	image_url =  y[0]["url"]
	image_name =  y[0]["name"]
	filename= '/home/nvidia/pizza/images/'+image_name



	if image_name != oldImageName :
		
		if oldImageName != "name":				
		
			r = requests.get(image_url, allow_redirects=True)
			open(filename, 'wb').write(r.content)
		

			print("new image saved")

			upload_rating.rate(image_name)

			oldImageName = image_name
		else :

			
			oldImageName = image_name


	else:
		print('Wear long pants.')



# the result is a Python dictionary:
#print(z)

#print(response.text)

