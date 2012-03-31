/*
 * This Source Code is subject to the terms of the Mozilla Public License
 * version 2.0 (the "License"). You can obtain a copy of the License at
 * http://mozilla.org/MPL/2.0/.
 */

function reportError(exp)
{
  Android.print("Error: " + exp);
  if (typeof exp == "string")
  {
    Android.showToast(exp);
  }
  Android.print(exp.stack);
}

function MatcherPatch()
{
  // Very ugly - we need to rewrite _checkEntryMatch() function to make sure
  // it calls Filter.fromText() instead of assuming that the filter exists.
  var origFunction = Matcher.prototype._checkEntryMatch.toString();
  var newFunction = origFunction.replace(/\bFilter\.knownFilters\[(.*?)\];/g, "Filter.fromText($1);");
  eval("Matcher.prototype._checkEntryMatch = " + newFunction);
}


var window = this;

var Components =
{
  interfaces:
  {
    nsIFile: {DIRECTORY_TYPE: 0},
    nsIFileURL: function() {},
    nsIFileInputStream: null,
    nsIFileOutputStream: null,
    nsIHttpChannel: function() {},
    nsIConverterInputStream: {DEFAULT_REPLACEMENT_CHARACTER: null},
    nsIConverterOutputStream: null,
    nsIUnicharLineInputStream: null,
    nsISafeOutputStream: null,
    nsITimer: {TYPE_REPEATING_SLACK: 0},
    nsIInterfaceRequestor: null,
    nsIChannelEventSink: null
  },
  classes:
  {
    "@mozilla.org/network/file-input-stream;1":
    {
      createInstance: function()
      {
        return new FakeInputStream();
      }
    },
    "@mozilla.org/network/file-output-stream;1":
    {
      createInstance: function()
      {
        return new FakeOutputStream();
      }
    },
    "@mozilla.org/dom/json;1":
    {
      createInstance: function() {
        return {
          decodeFromStream: function(stream, encoding)
          {
            var line = {};
            var haveMore = true;
            var s = new String();
            while (true)
            {
              if (haveMore)
                haveMore = stream.readLine(line);
              else
                break;
              s += line.value;
            }
          	return JSON.parse(s);
          },
          encodeToStream: function(stream, encoding, something, obj)
          {
            var s = JSON.stringify(obj);
            stream.writeString(s);
          }
        }
      }
    },
    "@mozilla.org/timer;1":
    {
      createInstance: function()
      {
        return new FakeTimer();
      }
    }
  },
  results: {},
  utils: {
    reportError: reportError
  },
  manager: null,
  ID: function()
  {
    return null;
  },
  Constructor: function()
  {
    // This method is only used to get XMLHttpRequest constructor
    return XMLHttpRequest;
  }
};
const Cc = Components.classes;
const Ci = Components.interfaces;
const Cr = Components.results;
const Cu = Components.utils;

Cc["@mozilla.org/intl/converter-input-stream;1"] = Cc["@mozilla.org/network/file-input-stream;1"];
Cc["@mozilla.org/network/safe-file-output-stream;1"] = Cc["@mozilla.org/intl/converter-output-stream;1"] = Cc["@mozilla.org/network/file-output-stream;1"];

var Prefs =
{
  patternsbackups: 5,
  patternsbackupinterval: 24,
  data_directory: _datapath,
  savestats: false,
  privateBrowsing: false,
  subscriptions_fallbackerrors: 5,
  subscriptions_fallbackurl: "https://adblockplus.org/getSubscription?version=%VERSION%&url=%SUBSCRIPTION%&downloadURL=%URL%&error=%ERROR%&channelStatus=%CHANNELSTATUS%&responseStatus=%RESPONSESTATUS%",
  addListener: function() {}
};

var Utils =
{
  systemPrincipal: null,
  getString: function(id)
  {
    return id;
  },
  getLineBreak: function()
  {
    return "\n";
  },
  resolveFilePath: function(path)
  {
    return new FakeFile(path);
  },
  ioService:
  {
    newURI: function(uri)
    {
      if (!uri.length || uri[0] == "~")
        throw new Error("Invalid URI");

      /^([^:\/]*)/.test(uri);
      var scheme = RegExp.$1.toLowerCase();

      return {scheme: scheme, spec: uri};
    }
  },
  observerService:
  {
    addObserver: function() {},
    removeObserver: function() {}
  },
  chromeRegistry:
  {
    convertChromeURL: function() {}
  },
  runAsync: function(callback, thisPtr)
  {
    var params = Array.prototype.slice.call(arguments, 2);
    Android.setTimeout(function()
    {
      callback.apply(thisPtr, params);
    }, 0);
  },
  addonVersion: _version,
  platformVersion: "10.0",
  get appLocale()
  {
    Android.getLocale();
  },
  generateChecksum: function(lines)
  {
    // We cannot calculate MD5 checksums yet :-(
    return null;
  },
  makeURI: function(url)
  {
    return Utils.ioService.newURI(url);
  },
  checkLocalePrefixMatch: function(prefixes)
  {
    if (!prefixes)
      return null;

    var list = prefixes.split(",");
    for (var i = 0; i < list.length; i++)
      if (new RegExp("^" + list[i] + "\\b").test(Utils.appLocale))
        return list[i];

    return null;
  },
  versionComparator:
  {
    compare: function(v1, v2)
    {
      var parts1 = v1.split(".");
      var parts2 = v2.split(".");
      for (var i = 0; i < Math.max(parts1.length, parts2.length); i++)
      {
        // TODO: Handle non-integer version parts properly
        var part1 = parseInt(i < parts1.length ? parts1[i] : "0");
        var part2 = parseInt(i < parts2.length ? parts2[i] : "0");
        if (part1 != part2)
          return part1 - part2;
      }
      return 0;
    }
  }
};

var XPCOMUtils =
{
  generateQI: function() {}
};

function FakeFile(path)
{
  this.path = path;
}
FakeFile.prototype =
{
  get leafName()
  {
    return this.path;
  },
  set leafName(value)
  {
    this.path = value;
  },
  append: function(path)
  {
    this.path += _separator + path;
  },
  clone: function()
  {
    return new FakeFile(this.path);
  },
  exists: function()
  {
    return Android.fileExists(this.path);
  },
  remove: function()
  {
    Android.fileRemove(this.path);
  },
  moveTo: function(parent, newPath)
  {
    Android.fileRename(this.path, newPath);
  },
  get lastModifiedTime()
  {
    return Android.fileLastModified(this.path);
  },
  get parent()
  {
    return {create: function() {}};
  },
  normalize: function() {}
};

function FakeInputStream()
{
}
FakeInputStream.prototype =
{
  lines: null,
  currentIndex: 0,

  init: function(file)
  {
    if (file instanceof FakeInputStream)
      this.lines = file.lines;
    else
      this.lines = Android.fileRead(file.path).split(/\n/);
  },
  readLine: function(line)
  {
    if (this.currentIndex < this.lines.length)
      line.value = this.lines[this.currentIndex];
    this.currentIndex++;
    return (this.currentIndex < this.lines.length);
  },
  close: function() {},
  QueryInterface: function()
  {
    return this;
  }
};

function FakeOutputStream()
{
}
FakeOutputStream.prototype =
{
  file: null,
  buffer: null,

  init: function(file)
  {
    if (file instanceof FakeOutputStream)
    {
      this.file = file.file;
      this.buffer = file.buffer;
    }
    else
    {
      this.file = file;
      this.buffer = [];
    }
  },
  writeString: function(string)
  {
    this.buffer.push(string);
  },
  close: function()
  {
    Android.fileWrite(this.file.path, this.buffer.join(""));
  },
  finish: function()
  {
    this.close();
  },
  flush: function() {},
  QueryInterface: function()
  {
    return this;
  }
};

function FakeTimer()
{
}
FakeTimer.prototype =
{
  delay: 0,
  callback: null,
  initWithCallback: function(callback, delay)
  {
    this.callback = callback;
    this.delay = delay;
    this.scheduleTimeout();
  },
  scheduleTimeout: function()
  {
    var me = this;
    Android.setTimeout(function()
    {
      try
      {
        me.callback();
      }
      catch(e)
      {
        reportError(e);
      }
      me.scheduleTimeout();
    }, this.delay);
  }
};

var ElemHide =
{
  init: function() {},
  apply: function() {},
  add: function() {},
  remove: function() {},
  clear: function() {},
  toCache: function() {},
  fromCache: function() {}
};

function clearSubscriptions()
{
  while (FilterStorage.subscriptions.length)
    FilterStorage.removeSubscription(FilterStorage.subscriptions[0]);
}

function addSubscription(jsonSub)
{
  var newSub = JSON.parse(jsonSub);

  var subscription = Subscription.fromURL(newSub["url"]);
  if (subscription)
  {
    subscription.disabled = false;
    subscription.title = newSub["title"];
    subscription.homepage = newSub["homepage"];
    if (subscription instanceof DownloadableSubscription && !subscription.lastDownload)
    {
      Synchronizer.execute(subscription);
    }
    FilterStorage.addSubscription(subscription);
    FilterStorage.saveToDisk();
  }
}

function checkSubscriptions()
{
  for (var i = 0; i < FilterStorage.subscriptions.length; i++)
  {
    var subscription = FilterStorage.subscriptions[i];
    Android.print(subscription.title);
    if (subscription instanceof DownloadableSubscription)
    {
      updateSubscriptionStatus(subscription);
      if (!subscription.lastDownload)
      {
        Synchronizer.execute(subscription);
      }
    }
  }
}

function updateSubscriptionStatus(subscription)
{
  var status = "";
  var time = 0;
  if (Synchronizer.isExecuting(subscription.url))
    status = "synchronize_in_progress";
  else if (subscription.downloadStatus && subscription.downloadStatus != "synchronize_ok")
    status = subscription.downloadStatus;
  else if (subscription.lastDownload > 0)
  {
    time = subscription.lastDownload * 1000;
    status = "synchronize_last_at";
  }
  else
    status = "synchronize_never";
    
  Android.setStatus(status, time);
}

function onFilterChange(action, subscription, param1, param2)
{
  switch (action)
  {
    case "subscription.lastDownload":
    case "subscription.downloadStatus":
      updateSubscriptionStatus(subscription);
      break;
  }
}

function unloadOptions()
{
  FilterNotifier.removeListener(onFilterChange);
}

Android.load("XMLHttpRequest.jsm");
Android.load("FilterNotifier.jsm");
Android.load("FilterClasses.jsm");
Android.load("SubscriptionClasses.jsm");
Android.load("FilterStorage.jsm");
Android.load("FilterListener.jsm");
Android.load("Matcher.jsm");
Android.load("Synchronizer.jsm");

FilterListener.startup();
//Synchronizer.startup();

FilterNotifier.addListener(onFilterChange);

Android.showToast("ready");

var hasSubscriptions = FilterStorage.subscriptions.some(function(subscription) {return subscription instanceof DownloadableSubscription});
