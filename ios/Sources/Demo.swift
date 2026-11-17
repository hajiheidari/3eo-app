import Foundation

/// Screenshot mode (launch arguments `-demo -screen <name>`): the app runs on invented sample data and never touches the network.
enum Demo {
    static let on = ProcessInfo.processInfo.arguments.contains("-demo")
    static var screen: String {
        let a = ProcessInfo.processInfo.arguments
        if let i = a.firstIndex(of: "-screen"), i + 1 < a.count { return a[i + 1] }
        return "home"
    }
    static let lang: String? = {
        let a = ProcessInfo.processInfo.arguments
        if let i = a.firstIndex(of: "-lang"), i + 1 < a.count { return a[i + 1] }
        return nil
    }()
    static var initialTab: Int {
        switch screen { case "reports": return 1; case "account": return 2; case "pro": return 3; case "blog": return 4; default: return 0 }
    }

    static func json(_ path: String) -> String {
        if path == "/api/app/me" {
            return #"{"ok":true,"mobile":"0912***4567","credits":86,"admin":false,"mode":"seoer","reports":[{"token":"a1","type":"analyze","host":"karno-shop.ir","score":82,"created_at":"2026-11-15 10:20:00"},{"token":"a2","type":"speed","host":"karno-shop.ir","score":64,"created_at":"2026-11-14 09:00:00"},{"token":"a3","type":"ai","host":"paya-law.ir","score":71,"created_at":"2026-11-12 16:40:00"},{"token":"a4","type":"analyze","host":"paya-law.ir","score":58,"created_at":"2026-11-10 12:10:00"},{"token":"a5","type":"authority","host":"sabz-cafe.ir","score":77,"created_at":"2026-11-09 18:30:00"}]}"#
        }
        if path == "/api/app/tools" { return tools }
        if path == "/api/app/clients" {
            return #"{"ok":true,"max":20,"cost":2,"credits":86,"clients":[{"id":1,"name":"فروشگاه کارنو","host":"karno-shop.ir","note":"","score":82,"prev":76,"last":"2026-11-15","reports":6,"auto":2,"paused":0},{"id":2,"name":"دفتر وکالت پایا","host":"paya-law.ir","note":"","score":71,"prev":71,"last":"2026-11-12","reports":4,"auto":1,"paused":0},{"id":3,"name":"کافه سبز","host":"sabz-cafe.ir","note":"","score":58,"prev":49,"last":"2026-11-10","reports":3,"auto":0,"paused":0}]}"#
        }
        if path == "/api/app/rank" {
            return #"{"ok":true,"enabled":true,"max":10,"credits":86,"free":false,"cost":{"check":2,"daily":10,"weekly":3},"summary":{"avg":6.4,"top3":2,"top10":4},"tracks":[{"id":1,"domain":"karno-shop.ir","keyword":"خرید کفش ورزشی","now":2,"delta":3,"best":2,"every":"daily","active":true},{"id":2,"domain":"karno-shop.ir","keyword":"کفش پیاده‌روی زنانه","now":3,"delta":1,"best":3,"every":"daily","active":true},{"id":3,"domain":"karno-shop.ir","keyword":"قیمت کتانی اصل","now":7,"delta":-2,"best":4,"every":"weekly","active":true},{"id":4,"domain":"karno-shop.ir","keyword":"کفش مردانه رسمی","now":9,"delta":4,"best":9,"every":"weekly","active":true},{"id":5,"domain":"karno-shop.ir","keyword":"فروشگاه آنلاین کفش","now":14,"delta":0,"best":11,"every":"weekly","active":true},{"id":6,"domain":"karno-shop.ir","keyword":"کفش کوهنوردی","now":null,"delta":null,"best":null,"every":"weekly","active":true}]}"#
        }
        if path == "/api/app/monitor" {
            return #"{"ok":true,"max":3,"cost":3,"credits":86,"free":false,"monitors":[{"id":1,"brand":"کارنو","domain":"karno-shop.ir","topic":"فروش کفش ورزشی","status":"active","runs":[{"score":68,"known":true},{"score":55,"known":true},{"score":40,"known":false}]},{"id":2,"brand":"دفتر وکالت پایا","domain":"paya-law.ir","topic":"وکیل ملکی","status":"active","runs":[{"score":47,"known":true},{"score":52,"known":true},{"score":31,"known":false}]},{"id":3,"brand":"کافه سبز","domain":"sabz-cafe.ir","topic":"کافه و صبحانه","status":"active","runs":[{"score":22,"known":false},{"score":18,"known":false}]}]}"#
        }
        if path == "/api/app/content/1" {
            return #"{"ok":true,"kind":"article","status":"done","title":"راهنمای انتخاب کفش پیاده‌روی","error":"","free":false,"article":{"title":"راهنمای کامل انتخاب کفش پیاده‌روی؛ ۷ نکته‌ای که قبل از خرید باید بدانید","meta":"کفش پیاده‌روی مناسب چطور انتخاب می‌شود؟ در این راهنما اندازه، کفی، جنس و قیمت را قدم‌به‌قدم بررسی می‌کنیم.","body":"انتخاب کفش مناسب پیاده‌روی فقط به ظاهر آن بستگی ندارد. کفشی که پا را درست نگه ندارد، بعد از چند هفته درد مچ و زانو می‌آورد.\n\nاول؛ اندازه‌ی پا را عصر اندازه بگیرید\nپا در طول روز کمی ورم می‌کند، پس بهترین زمان برای امتحان کفش عصر است. حدود یک سانتی‌متر فضای خالی جلوی انگشت بزرگ بگذارید.\n\nدوم؛ به کفی و بالشتک توجه کنید\nکفی نرم برای مسیرهای کوتاه خوب است، اما برای پیاده‌روی طولانی کفی نیمه‌سفت پایداری بیشتری می‌دهد.","words":820}}"#
        }
        return "{\"ok\":true}"
    }

    static let report = #"{"ok":true,"title":"تحلیل کامل سئو","host":"karno-shop.ir","type":"analyze","share":"x","token":"a1","blocks":[{"t":"score","score":82,"label":"امتیاز کلی سئوی karno-shop.ir"},{"t":"counts","pass":31,"warn":7,"fail":3},{"t":"h","text":"موارد مهم برای رفع"},{"t":"checks","categories":[{"title":"سئوی صفحه","score":78,"checks":[{"status":"warn","title":"توضیح متا کوتاه است","detail":"توضیح متای صفحه‌ی اصلی فقط ۶۲ نویسه دارد.","fix":"توضیح را به ۱۲۰ تا ۱۵۸ نویسه برسانید.","gain":2.5},{"status":"fail","title":"تصویر بدون متن جایگزین","detail":"۹ تصویر متن جایگزین (alt) ندارد.","fix":"برای هر تصویر یک توضیح کوتاه بنویسید.","gain":3.0},{"status":"pass","title":"عنوان صفحه مناسب است","detail":"طول عنوان ۵۴ نویسه است.","fix":"","gain":0}]},{"title":"فنی و ایندکس","score":90,"checks":[{"status":"pass","title":"فایل robots.txt درست است","detail":"خزندگان گوگل مسدود نشده‌اند.","fix":"","gain":0},{"status":"pass","title":"نقشه‌ی سایت موجود است","detail":"sitemap.xml پیدا شد.","fix":"","gain":0}]}]}]}"#

    static let blog = #"[{"id":1,"date":"2026-11-14T10:00:00","title":{"rendered":"۱۰ اشتباه رایج در سئوی سایت فروشگاهی"},"excerpt":{"rendered":"<p>این اشتباه‌ها بدون سروصدا رتبه‌ی فروشگاه شما را پایین می‌آورند؛ از عنوان‌های تکراری تا سرعت پایین موبایل.</p>"},"content":{"rendered":"<p>سایت فروشگاهی اگر ساختار درستی نداشته باشد، حتی با محصول عالی هم دیده نمی‌شود.</p><h2>۱. عنوان‌های تکراری</h2><p>هر صفحه‌ی محصول باید عنوان یکتا داشته باشد.</p>"},"link":"https://3eo.ir/blog/a","_embedded":null},{"id":2,"date":"2026-11-10T10:00:00","title":{"rendered":"فایل llms.txt چیست و آیا سایت شما به آن نیاز دارد؟"},"excerpt":{"rendered":"<p>هوش مصنوعی چطور سایت شما را می‌خواند و چه چیزی باید به آن نشان بدهید.</p>"},"content":{"rendered":"<p>متن</p>"},"link":"https://3eo.ir/blog/b","_embedded":null},{"id":3,"date":"2026-11-05T10:00:00","title":{"rendered":"سرعت سایت و رتبه‌ی گوگل؛ چه عددی کافی است؟"},"excerpt":{"rendered":"<p>معیارهای سرعت در سال جدید و ساده‌ترین راه‌های بهبود آن‌ها.</p>"},"content":{"rendered":"<p>متن</p>"},"link":"https://3eo.ir/blog/c","_embedded":null},{"id":4,"date":"2026-10-30T10:00:00","title":{"rendered":"چطور برند شما در پاسخ ChatGPT دیده شود؟"},"excerpt":{"rendered":"<p>چهار کاری که شانس معرفی برند شما را توسط هوش مصنوعی بیشتر می‌کند.</p>"},"content":{"rendered":"<p>متن</p>"},"link":"https://3eo.ir/blog/d","_embedded":null}]"#

    static var tools: String {
        let cats = #"{"tech":"فنی","keyword":"کلمات کلیدی","content":"محتوا","ai":"هوش مصنوعی","data":"داده و اعتبار"}"#
        let list: [(String, String, String, String, String)] = [
            ("analyze", "تحلیل کامل سئو", "امتیاز و فهرست مشکل‌های سئوی یک سایت", "tech", "رایگان"),
            ("speed", "سرعت سایت", "سرعت موبایل و Core Web Vitals", "tech", "رایگان"),
            ("robots", "بررسی robots.txt", "ربات‌ها مسدود نشده باشند", "tech", "رایگان"),
            ("httpcheck", "بررسی هدرها و HTTPS", "امنیت و کش سایت", "tech", "رایگان"),
            ("cluster", "خوشه‌بندی کلمات", "کلمات را گروه‌بندی کنید", "keyword", "رایگان"),
            ("suggest", "پیشنهاد کلمه‌ی کلیدی", "کلمات پرجستجو از گوگل", "keyword", "رایگان"),
            ("ideas", "ایده‌ی محتوا", "موضوع مقاله پیدا کنید", "content", "۲ اعتبار"),
            ("metadesc", "توضیح متا", "توضیح متای جذاب بسازید", "content", "۱ اعتبار"),
            ("ai", "دیده‌شدن در هوش مصنوعی", "آیا ChatGPT برند شما را می‌شناسد؟", "ai", "۳ اعتبار"),
            ("llmstxt", "فایل llms.txt", "سایت خود را به هوش مصنوعی معرفی کنید", "ai", "رایگان"),
            ("authority", "اعتبار دامنه", "سن و اعتبار دامنه", "data", "رایگان"),
            ("links", "بررسی لینک‌ها", "لینک‌های شکسته را پیدا کنید", "tech", "رایگان"),
        ]
        let items = list.map { "{\"slug\":\"\($0.0)\",\"title\":\"\($0.1)\",\"desc\":\"\($0.2)\",\"cat\":\"\($0.3)\",\"heavy\":false,\"cost\":\"\($0.4)\",\"paid\":false,\"fields\":[]}" }.joined(separator: ",")
        return "{\"ok\":true,\"categories\":\(cats),\"tools\":[\(items)]}"
    }

    static func respond<T: Decodable>(_ path: String, as type: T.Type) throws -> T {
        let s = localize(path.hasPrefix("/api/app/report/") ? report : json(path))
        return try JSONDecoder().decode(T.self, from: Data(s.utf8))
    }

    /// English screenshots: translate every string of the sample JSON
    static func localize(_ s: String) -> String {
        if L.isFa { return s }
        guard let d = s.data(using: .utf8), let o = try? JSONSerialization.jsonObject(with: d),
              let out = try? JSONSerialization.data(withJSONObject: walk(o)), let r = String(data: out, encoding: .utf8) else { return s }
        return r
    }

    private static func walk(_ v: Any) -> Any {
        if let s = v as? String { return DemoEn.d[s] ?? L.t(s) }
        if let a = v as? [Any] { return a.map { walk($0) } }
        if let d = v as? [String: Any] { return d.mapValues { walk($0) } }
        return v
    }
}
