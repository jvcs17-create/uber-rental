# Uber Rental Analyzer

App Android que analiza **en tiempo real** la rentabilidad de cada servicio que
llega a **Uber Driver**. Cuando aparece una oferta, la app lee la tarifa, el
tiempo y la distancia (incluyendo el trayecto de ida a recoger al pasajero),
calcula la **ganancia por hora** y **por km**, y muestra una tarjeta encima:

- 🟢 **VERDE = TOMAR** → ganancia por hora **≥ $25.000** (configurable)
- 🔴 **ROJO = NO CONVIENE** → por debajo del umbral

---

## ⚠️ Antes de usar — léelo

1. **Uber no tiene una API para esto.** La app usa el **Servicio de Accesibilidad**
   de Android para *leer el texto que ya ves en pantalla*. **Solo lee**, no acepta
   viajes por ti ni toca botones.
2. **Riesgo de cuenta.** Automatizar o leer sobre Uber Driver va contra sus
   términos. El riesgo con una app solo de lectura es bajo, pero **no es cero**.
   Úsala bajo tu propio criterio.
3. **No es para Google Play.** Se instala directamente en tu teléfono (sideload).
4. Los cálculos son **estimados** según lo que Uber muestra en la oferta.

---

## Cómo compilar el APK

Necesitas **Android Studio** (gratis). Es la forma más fácil.

1. Instala Android Studio: https://developer.android.com/studio
2. Abre Android Studio → **Open** → selecciona esta carpeta `UberRentalAnalyzer`.
3. Espera a que Gradle sincronice (baja las dependencias la primera vez).
4. Conecta tu teléfono Android por USB con **Depuración USB** activada
   (Ajustes → Opciones de desarrollador → Depuración USB).
5. Presiona **Run ▶** (o `Shift+F10`). La app se instala en tu teléfono.

> Alternativa por consola (si ya tienes el SDK de Android configurado):
> ```
> ./gradlew assembleDebug
> ```
> El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Cópialo al
> teléfono e instálalo (activa "Instalar apps de fuentes desconocidas").

Requisitos técnicos: **Android 8.0 (API 26) o superior**.

---

## Cómo usarla

1. Abre **Uber Rental Analyzer**.
2. Ajusta tu **umbral** (viene en $25.000/hora). Toca **Guardar**.
3. Toca **1. Activar accesibilidad** → busca "Uber Rental Analyzer" en la lista y
   actívalo.
4. Toca **2. Permitir mostrar encima** → activa el permiso.
5. Toca **Probar tarjeta de ejemplo** para ver cómo se ve el overlay.
6. Abre **Uber Driver** y ponte en línea. Cuando llegue una oferta, aparecerá la
   tarjeta verde o roja con la ganancia por hora y por km.
   - **Toca** la tarjeta para cerrarla.
   - **Arrástrala** para moverla de lugar.

---

## Si los números salen mal → Modo aprendizaje

La parte más delicada es **leer** correctamente la oferta, porque Uber cambia el
diseño de su pantalla cada tanto y usa textos distintos ("de distancia", "de
viaje", etc.).

Activa el interruptor **Modo aprendizaje** en la app. Entonces, al llegar una
oferta, la tarjeta mostrará también el **texto crudo** que la app leyó de la
pantalla. Con eso puedes ver exactamente qué está leyendo y ajustar las
expresiones de lectura en el archivo:

```
app/src/main/java/com/juanka/uberrental/OfferParser.kt
```

Ahí están los patrones (regex) para la tarifa, los minutos y los km, y las
palabras clave que distinguen el tramo de ida a recoger del viaje. Si me pasas
una captura o el texto crudo que muestra el modo aprendizaje, puedo afinar el
parser para tu versión de Uber Driver.

---

## Cómo funciona por dentro

```
Uber Driver muestra una oferta
        │
        ▼
UberAccessibilityService  ← lee todo el texto de la pantalla
        │
        ▼
OfferParser               ← saca tarifa, min y km (formato colombiano)
        │
        ▼
ProfitCalculator          ← $/hora = tarifa ÷ (min_total/60)
                            $/km   = tarifa ÷ km_total
        │                   (min_total y km_total incluyen la ida a recoger)
        ▼
OverlayManager            ← dibuja la tarjeta verde/roja encima
```

Archivos principales:

| Archivo | Qué hace |
|---|---|
| `UberAccessibilityService.kt` | Detecta y lee la oferta en Uber Driver |
| `OfferParser.kt` | Interpreta el texto (tarifa, tiempo, distancia) |
| `ProfitCalculator.kt` | Calcula ganancia por hora y por km |
| `OverlayManager.kt` | Muestra la tarjeta verde/roja |
| `MainActivity.kt` | Pantalla de configuración y permisos |
| `SettingsStore.kt` | Guarda el umbral y el modo aprendizaje |

---

## Ideas para versiones siguientes

- Restar costos (gasolina + desgaste) para ganancia **neta**.
- Historial de ofertas tomadas/rechazadas y resumen del día.
- Sonido o vibración distinta para verde vs rojo.
- Umbral por franja horaria.
