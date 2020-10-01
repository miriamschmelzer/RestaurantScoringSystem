
def rate(image_name, score):
    import requests
    up_url = "https://aiotlab.111mb.de/AndroidUploadImage/upload.php"
    up_payload = {'name':image_name , 'ai_rate': score}
    response = requests.request("POST", up_url, data=up_payload)


