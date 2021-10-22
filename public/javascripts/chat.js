function main() {
  const vm = new Vue({
    el: '#app',
    data: function() {
      return {
        id: new Date().getTime().toString(16)  + Math.floor(1000*Math.random()).toString(16),
        name: "",
        text: "",
        room: "A",
        messages: [],
        websocket: null,
      }
    },

    methods: {
      pushMessage: function(id, name, text) {
        this.messages.push({
          "id": id,
          "name": name,
          "text": text,
        });
      },

      entryRoom : function() {
        const self = this;
        this.websocket = new WebSocket(`ws://127.0.0.1:9000/chat/${this.room}`);

        this.websocket.onopen = function(event) {
          console.log("websocket onopen");
        };
  
        this.websocket.onmessage = function(event) {
          console.log("websocket onmessage");
          const data = JSON.parse(event.data);
          self.pushMessage(data.id, data.user, data.text);
        };
  
        this.websocket.onclose = function(event) {
          console.log("websocket.onclose");
        };
  
        this.websocket.onerror = function(event) {
          console.log("websocket onerror");
        };

        this.connected = true;
        console.log(this.websocket);
      },

      sendMessage : function() {
        if (this.websocket) {
          this.websocket.send(JSON.stringify({id: this.id, user: this.name, text: this.text}));
          this.text = "";
        };
      },

      exitRoom : function() {
        this.websocket.close();
        this.websocket = null;
        this.connected = false;
        this.text = "";
        this.clearMessage();
      },

      clearMessage : function() {
        this.messages = [];
      }
    }
  });
};
