# Guía paso a paso — Uber Rental Analyzer

Esta guía es para alguien que **nunca ha compilado una app**. Vamos con calma.
El objetivo es que termines con la app instalada en tu teléfono Android,
mostrando la tarjeta 🟢 verde / 🔴 roja encima de Uber Driver.

> **Necesitas un teléfono Android** (versión 8.0 o superior). En iPhone **no**
> funciona: Apple no deja que una app lea la pantalla de otra ni dibuje encima.

---

## Antes de empezar — lee esto (importante)

- La app **solo lee** lo que Uber ya te muestra en pantalla para calcular la
  rentabilidad. **No acepta viajes por ti** ni toca botones.
- Leer datos sobre Uber Driver va **contra sus términos de servicio**. El riesgo
  con una app solo de lectura es bajo, pero **no es cero**. La usas bajo tu criterio.
- No va en Google Play; se instala directo en tu teléfono (esto se llama *sideload*).
- Los cálculos son **estimados** según lo que Uber muestra.

---

# PARTE 1 — Conseguir el APK (el archivo instalable)

Como no tienes las herramientas de programación instaladas, la forma más fácil es
dejar que **GitHub compile la app gratis en la nube** y tú solo descargas el
resultado. No tienes que instalar nada pesado en tu computador.

## Opción A (RECOMENDADA): compilar en la nube con GitHub

### Paso 1 — Crear una cuenta de GitHub (gratis)
1. Entra a https://github.com y haz clic en **Sign up**.
2. Crea tu cuenta con tu correo (jvcs17@gmail.com sirve) y una contraseña.
3. Confirma el correo.

### Paso 2 — Crear un repositorio (una "carpeta" en la nube)
1. Arriba a la derecha, clic en el **+** → **New repository**.
2. En *Repository name* escribe: `uber-rental`
3. Déjalo en **Public** (o Private, da igual).
4. **NO** marques "Add a README".
5. Clic en **Create repository**.

### Paso 3 — Subir el proyecto
1. En la página del repo recién creado, busca el enlace
   **"uploading an existing file"** (o el botón **Add file → Upload files**).
2. Abre la carpeta `UberRentalAnalyzer` en tu computador.
3. **Selecciona TODO lo que hay dentro** (todas las carpetas y archivos:
   `app`, `gradle`, `.github`, `build.gradle.kts`, `gradlew`, etc.) y
   **arrástralo** a la ventana de GitHub.
   - Importante: arrastra el **contenido** de la carpeta, no la carpeta en sí,
     para que `build.gradle.kts` quede en la raíz del repositorio.
   - Si no ves la carpeta `.github`, activa "mostrar archivos ocultos" en tu
     explorador, o usa el botón *Add file → Upload files* y arrastra también esa carpeta.
4. Abajo, clic en **Commit changes**.

### Paso 4 — Esperar a que compile sola
1. En el repo, entra a la pestaña **Actions** (arriba).
2. Verás un proceso llamado **"Compilar APK"** ejecutándose (círculo amarillo).
   Tarda unos **3 a 6 minutos** la primera vez.
3. Cuando termine con un ✅ verde, haz clic en ese proceso.

> Si te aparece un botón verde **"I understand my workflows, enable them"**,
> haz clic para permitir que Actions se ejecute. Luego entra a Actions otra vez;
> si no arrancó solo, clic en **"Compilar APK" → Run workflow**.

### Paso 5 — Descargar el APK
1. Dentro del proceso terminado, baja hasta la sección **Artifacts**.
2. Descarga **UberRental-APK**. Es un `.zip`.
3. Descomprímelo: adentro está **`app-debug.apk`**. Ese es tu app.
4. Pásalo a tu teléfono (por WhatsApp a ti mismo, cable USB, Google Drive, o
   descárgalo directamente desde el navegador del teléfono).

➡️ **Salta a la PARTE 2** para instalarlo.

---

## Opción B (alternativa): compilar en tu computador con Android Studio

Solo si prefieres hacerlo local. Es una descarga grande (~1 GB).

1. Descarga e instala **Android Studio**: https://developer.android.com/studio
2. Ábrelo → **Open** → selecciona la carpeta `UberRentalAnalyzer`.
3. Espera a que "Gradle sync" termine (baja dependencias la primera vez).
4. Menú **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Cuando termine, clic en **locate** para encontrar `app-debug.apk`.
6. Pásalo a tu teléfono.

---

# PARTE 2 — Instalar el APK en tu teléfono

1. En el teléfono, abre el archivo **`app-debug.apk`** (desde Archivos o Descargas).
2. Android te dirá que no puede instalar apps de origen desconocido.
   Toca **Ajustes** y activa **"Permitir de esta fuente"** (para tu navegador o
   la app de Archivos). Vuelve atrás.
3. Toca **Instalar**. Si sale "Play Protect", elige **Instalar de todos modos**.
4. Verás la app **Uber Rental Analyzer** en tu pantalla.

---

# PARTE 3 — Dar los 2 permisos (una sola vez)

Abre la app **Uber Rental Analyzer**. Verás dos botones de permisos:

### Permiso 1 — Mostrar encima de otras apps
1. Toca **"2. Permitir mostrar encima"**.
2. Activa el interruptor para **Uber Rental Analyzer**.
3. Vuelve a la app. El estado debe decir **ACTIVO** en verde.

### Permiso 2 — Accesibilidad (para leer la oferta)
1. Toca **"1. Activar accesibilidad"**.
2. En la lista, busca **Uber Rental Analyzer**, entra y **actívalo**.
3. Acepta el aviso de Android (le explica que la app leerá el contenido de la pantalla).
4. Vuelve a la app. El estado debe decir **ACTIVO** en verde.

> Estos permisos son los que hacen posible leer la oferta y dibujar la tarjeta.
> Sin ellos la app no puede funcionar.

---

# PARTE 4 — Usar la app

1. En la app puedes ajustar el **umbral** (viene en **25000**). Si quieres otro
   valor, cámbialo y toca **Guardar**. Verde = ganas ≥ ese valor por hora.
2. Toca **"Probar tarjeta de ejemplo"** para ver cómo se ve la tarjeta verde.
3. Abre **Uber Driver** normalmente y ponte en línea.
4. Cuando llegue un servicio, encima de la oferta aparecerá la tarjeta:
   - 🟢 **TOMAR** → ganancia por hora ≥ tu umbral.
   - 🔴 **NO CONVIENE** → por debajo.
   - Muestra **$/hora**, **$/km** y el detalle (tarifa · tiempo · km).
5. La tarjeta se puede **arrastrar** para moverla, y **tocarla** para cerrarla.
   Se cierra sola a los ~15 segundos.

---

# PARTE 5 — Si los números salen mal (ajustar la lectura)

Uber cambia el diseño de la oferta según ciudad y versión, así que la lectura
puede necesitar un ajuste fino. Para eso está el **Modo aprendizaje**:

1. En la app, activa el interruptor **"Modo aprendizaje"**.
2. Recibe (o simula) una oferta en Uber Driver.
3. La tarjeta mostrará el **texto crudo** que la app leyó de la pantalla.
4. Tómale una **captura de pantalla**.
5. Envíame esa captura por el chat y yo **ajusto el lector** para que tome
   exactamente los valores de tu Uber. Te devuelvo el proyecto corregido y
   vuelves a compilar (repites la Parte 1, Paso 3 en adelante).

---

# PARTE 6 — Prueba gratis y licencia

**La app arranca con 15 días GRATIS automáticos.** No tienes que hacer nada: al
instalarla y activarla, empiezas tu prueba y verás arriba "🎁 Prueba GRATIS activa
· quedan N días". Cuando se acaben los 15 días, la app te pedirá un **código de
activación** (de pago, 30 días) para seguir usándola; sin código válido, al llegar
una oferta solo verás una tarjeta gris de "licencia inactiva".

**Para activar un código de pago (cuando termine la prueba):**

1. Abre la app. En la sección **"Licencia de uso"** verás tu **ID de equipo**
   (una fila de letras y números).
2. Toca **"Copiar ID"** y envíaselo al vendedor (por WhatsApp).
3. El vendedor te devuelve un **código** largo. Cópialo.
4. En la app, toca **"Pegar"** (pega el código) y luego **"Activar"**.
5. Si todo está bien, verás en verde: **"Licencia ACTIVA · vence dd/mm/aaaa
   (faltan N días)"**. ¡Listo!
6. A los 30 días el código vence y la app vuelve a pedir uno nuevo. Renueva
   pidiéndole otro código al vendedor.

> El código está **atado a tu teléfono**: solo funciona en el equipo cuyo ID
> enviaste. No se puede compartir con otros.

---

# PARTE 7 — Que no se apague sola (importante en Xiaomi/Redmi/POCO)

Android, y sobre todo Xiaomi, tiende a "matar" apps en segundo plano. Para que la
app siga leyendo las ofertas de forma confiable:

1. En la app, toca **"Quitar optimización de batería"** y acepta. El estado debe
   quedar en verde: "Batería: sin restricción ✓".
2. Solo en **Xiaomi/Redmi/POCO (MIUI/HyperOS)**: entra a **Ajustes → Aplicaciones
   → Uber Rental Analyzer → Ahorro de batería → Sin restricciones**, y activa
   **"Inicio automático" (Autostart)**. Esto no se puede hacer desde la app; toca
   hacerlo a mano una vez.
3. La app deja una notificación fija ("Uber Rental activo"). Es normal y necesaria
   para que Android no la cierre; puedes ignorarla.

> **Sobre Waze/Maps:** esta app **no interrumpe tu navegación**. Solo lee la
> pantalla de Uber y dibuja la tarjeta; **nunca** te saca de Waze ni cambia de app
> sola. (A diferencia de otras apps que sí fuerzan el cambio a Uber.)

---

# Problemas frecuentes

- **No aparece la tarjeta:** revisa que los DOS permisos estén en ACTIVO (Parte 3)
  y que Uber Driver esté abierto en primer plano.
- **La tarjeta sale pero los valores dicen "—":** la lectura no encontró los
  datos; usa el **Modo aprendizaje** (Parte 5) y mándame la captura.
- **Play Protect no me deja instalar:** toca "Más detalles" → "Instalar de todos
  modos". Es normal en apps fuera de la Play Store.
- **En Actions salió una X roja:** entra al proceso, cuéntame el error (o mándame
  captura) y lo corrijo.

---

¿Dudas en cualquier paso? Escríbeme el **número del paso** y qué ves en pantalla
(una captura ayuda muchísimo) y te destrabo.
