# **Domača naloga za računalsniške komunikacije - PROGRAMIRANJE VTIČEV:**
Navodila:

Delovanje pogovornega strežnika in odjemalca

Strežnik in odjemalec morata na posamezne tipe sporočil primerno odreagirati . Delo si boste olajšali, če boste vsako sporočilo tudi izpisali v komandno vrstico.

Vsa sporočila, ki se pošiljajo med strežnikom in odjemalci, morajo biti strukturirana.

Uporabnik se mora v odjemalca prijaviti z imenom, odjemalec nato na strežnik sporoči ime uporabnika.

Odjemalec naj omogoča uporabniku pošiljanje javnih in privatnih sporočil. Sami se odločite, kako bo uporabnik preko komandne vrstice določil tip sporočila, naslovnika in besedilo sporočila.

Strežnik mora javna in privatna sporočila ustrezno pošiljati na odjemalce.

V kolikor strežnik ne more najti naslovnika privatnega sporočila, naj pošiljatelju pošlje pošiljatelju sporočilo o napaki.

Vsako sporočilo, ki ga strežnik pošlje odjemalcu, mora biti opremljeno z imenom pošiljatelja.


## IMPLEMENTACIJA VIŠJE VARNOSTI
V tej nalogi boste naredili varnostno nadgradnjo pogovornega strežnika in odjemalca iz LDN7.
Odločili smo se, da bomo za višjo stopnjo varnosti poskrbeli s pomočjo protokola TLS in z uporabo samopodpisanih digitalnih certifikatov (seveda pa vse deluje tudi s "pravimi" certifikati):

### Strežnik se mora odjemalcu predstaviti z digitalnim certifikatom
>V polju Common Name strežnikovega certifikata naj se nahaja vrednost localhost


### Tudi odjemalci se morajo predstaviti strežniku z digitalnim certifikatom!
>Zgenerirajte digitalne certifikate za vsaj 3 odjemalce - uporabite različne vrednosti za Common Name
>
>Vrednost Common Name naj se uporablja na strežniku za identifikacijo odjemalca

Uporabnikovo ime naj se z odjemalca ne pošilja več na strežnik kot del sporočila. Strežnik naj odjemalca identificira na podlagi vrednosti v polju Common Name.

Strežnik naj dovoli priklop le z uporabo TLS 1.2 in kombinacijo naslednjih algoritmov ter dolžin ključev:
- ECDHE za izmenjavo klučev
- RSA
- AES z 128 bitnim ključem in uporabo GCM
- SHA256 za računanje HMAC
  
Takšen kandidat je le eden - https://ciphersuite.info/cs/TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256/
